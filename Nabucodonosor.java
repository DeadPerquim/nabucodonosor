package master_t;

import robocode.*;
import java.awt.Color;
import java.awt.Graphics2D;
import static robocode.util.Utils.normalRelativeAngleDegrees;

public class Nabucodonosor extends AdvancedRobot {

    // Variáveis de controle
    private boolean travarRadar = false;
    private double distanciaAlvo = 0;
    private long ultimoScan = 0;
    private String alvoAtual = null;
    private double larguraArena;
    private double alturaArena;
    private double margem = 60;
    private boolean evitandoParede = false;
    private final double DISTANCIA_MAXIMA_TIRO = 500;
    private boolean zigZag = false;
    private double alvoX = -1, alvoY = -1;
    private long tempoInicio;

    public void run() {
        setColors(Color.black, Color.red, Color.orange);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        larguraArena = getBattleFieldWidth();
        alturaArena = getBattleFieldHeight();
        tempoInicio = getTime();

        buscarAlvo();

        while (true) {
            if (travarRadar && (getTime() - ultimoScan > 30)) {
                buscarAlvo();
            }

            if (travarRadar) {
                double angulo = normalRelativeAngleDegrees(90 + (distanciaAlvo / 15));
                if (zigZag) {
                    setTurnRight(angulo);
                } else {
                    setTurnLeft(angulo);
                }
                setAhead(100);
                zigZag = !zigZag;
            } else {
                double proporcao = larguraArena / alturaArena;
                if (proporcao > 1.2) {
                    setTurnRight(45);
                } else if (proporcao < 0.8) {
                    setTurnRight(25);
                } else {
                    setTurnRight(35);
                }
                setAhead(100);
            }

            evitarParedes();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (!travarRadar || e.getDistance() < distanciaAlvo) {
            travarRadar = true;
            distanciaAlvo = e.getDistance();
            ultimoScan = getTime();
            alvoAtual = e.getName();

            out.println("📡 Alvo travado: " + alvoAtual);

            double anguloAbsoluto = getHeading() + e.getBearing();
            double anguloCanhao = normalRelativeAngleDegrees(anguloAbsoluto - getGunHeading());
            double anguloRadar = normalRelativeAngleDegrees(anguloAbsoluto - getRadarHeading());

            setTurnRadarRight(anguloRadar);
            setTurnGunRight(anguloCanhao);

            boolean miraAlinhada = Math.abs(anguloCanhao) < 10;
            boolean distanciaOk = distanciaAlvo <= DISTANCIA_MAXIMA_TIRO;
            boolean energiaOk = getEnergy() > 0.5;
            boolean economizarEnergia = getEnergy() < 45;

            if (miraAlinhada && distanciaOk && energiaOk) {
                double potencia = economizarEnergia ? 1.0 : Math.min(3.0, getEnergy());

                out.println("🔫 Disparando com potência " + potencia);
                setColors(Color.red, Color.orange, Color.yellow); // muda cor ao atirar

                double velocidadeInimigo = e.getVelocity();
                double direcaoInimigo = Math.toRadians(e.getHeading());
                double posXInimigo = getX() + Math.sin(Math.toRadians(e.getBearing() + getHeading())) * e.getDistance();
                double posYInimigo = getY() + Math.cos(Math.toRadians(e.getBearing() + getHeading())) * e.getDistance();

                double velocidadeTiro = 20 - 3 * potencia;
                double tempo = e.getDistance() / velocidadeTiro;

                double futuroX = posXInimigo + Math.sin(direcaoInimigo) * velocidadeInimigo * tempo;
                double futuroY = posYInimigo + Math.cos(direcaoInimigo) * velocidadeInimigo * tempo;

                double anguloPrevisto = Math.toDegrees(Math.atan2(futuroX - getX(), futuroY - getY()));
                double ajusteCanhao = normalRelativeAngleDegrees(anguloPrevisto - getGunHeading());

                setTurnGunRight(ajusteCanhao);
                fire(potencia);

                alvoX = futuroX;
                alvoY = futuroY;
            }
        }
    }

    public void onHitRobot(HitRobotEvent e) {
        setBack(50);
        setTurnRight(45);
        execute();
    }

    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(alvoAtual)) {
            buscarAlvo();
        }
    }

    public void onHitWall(HitWallEvent e) {
        evitandoParede = true;
        setBack(80);
        setTurnRight(90);
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        out.println("💥 Fui atingido! Iniciando manobra evasiva...");
        setColors(Color.blue, Color.cyan, Color.lightGray); // muda cor ao ser atingido
        zigZag = !zigZag;
        if (getEnergy() > 20) {
            setBack(30);
            setTurnRight(zigZag ? 45 : -45);
        } else {
            setAhead(40);
            setTurnLeft(zigZag ? 60 : -60);
        }
        execute();
    }

    public void onWin(WinEvent e) {
        out.println("🏆 Vitória! O campo de batalha é meu.");
        setColors(Color.green, Color.white, Color.magenta); // muda cor ao vencer

        // Dança da vitória
        for (int i = 0; i < 36; i++) {
            setTurnRight(10);
            setTurnGunLeft(20);
            setTurnRadarRight(30);
            execute();
        }
    }

    public void onPaint(Graphics2D g) {
        // Desenha linha até o inimigo previsto
        if (alvoX != -1 && alvoY != -1) {
            g.setColor(Color.red);
            g.drawLine((int) getX(), (int) getY(), (int) alvoX, (int) alvoY);
            g.setColor(Color.pink);
            g.drawOval((int) getX() - 250, (int) getY() - 250, 500, 500); // alcance máximo
        }
    }

    private void buscarAlvo() {
        travarRadar = false;
        alvoAtual = null;
        distanciaAlvo = Double.MAX_VALUE;
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    private void evitarParedes() {
        evitandoParede = false;
        if (getX() < margem) {
            setTurnRight(normalRelativeAngleDegrees(90 - getHeading()));
            evitandoParede = true;
        }
        if (getX() > larguraArena - margem) {
            setTurnRight(normalRelativeAngleDegrees(270 - getHeading()));
            evitandoParede = true;
        }
        if (getY() < margem) {
            setTurnRight(normalRelativeAngleDegrees(0 - getHeading()));
            evitandoParede = true;
        }
        if (getY() > alturaArena - margem) {
            setTurnRight(normalRelativeAngleDegrees(180 - getHeading()));
            evitandoParede = true;
        }

        if (evitandoParede) {
            setAhead(80);
        }
    }
}
