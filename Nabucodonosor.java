package master_t;

import robocode.*;
import java.awt.Color;
import java.awt.Graphics2D;
import static robocode.util.Utils.normalRelativeAngleDegrees;

public class Nabucodonosor extends AdvancedRobot {

    // Variáveis de controle
    private boolean travarRadar = false; // Indica se o radar está travado em um alvo
    private double distanciaAlvo = 0; // Distância atual do inimigo travado
    private long ultimoScan = 0; // Último tick em que o inimigo foi escaneado
    private String alvoAtual = null; // Nome do inimigo atual
    private double larguraArena; // Largura da arena
    private double alturaArena; // Altura da arena
    private double margem = 60; // Margem de segurança das paredes
    private boolean evitandoParede = false; // Indica se o robô está evitando parede
    private final double DISTANCIA_MAXIMA_TIRO = 500; // Distância máxima para tentar disparar
    private boolean zigZag = false; // Controla movimento em zigue-zague
    private double alvoX = -1, alvoY = -1; // Posição prevista do inimigo
    private long tempoInicio; // Tick inicial da partida

    public void run() {
        // Define as cores iniciais do robô
        setColors(Color.black, Color.red, Color.orange);
        // Desacopla a rotação do corpo, canhão e radar
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        // Captura as dimensões da arena
        larguraArena = getBattleFieldWidth();
        alturaArena = getBattleFieldHeight();
        tempoInicio = getTime();

        // Inicia radar em modo busca
        buscarAlvo();

        while (true) {
            // Se perdeu o inimigo, volta para busca
            if (travarRadar && (getTime() - ultimoScan > 30)) {
                buscarAlvo();
            }

            // Movimento de caça
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
                // Movimento de busca adaptado ao formato da arena
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
        // Decide travar novo alvo se ainda não travado ou se está mais próximo
        if (!travarRadar || e.getDistance() < distanciaAlvo) {
            travarRadar = true;
            distanciaAlvo = e.getDistance();
            ultimoScan = getTime();
            alvoAtual = e.getName();

            out.println("📡 Alvo travado: " + alvoAtual);

            // Calcula os ângulos de ajuste
            double anguloAbsoluto = getHeading() + e.getBearing();
            double anguloCanhao = normalRelativeAngleDegrees(anguloAbsoluto - getGunHeading());
            double anguloRadar = normalRelativeAngleDegrees(anguloAbsoluto - getRadarHeading());

            setTurnRadarRight(anguloRadar);
            setTurnGunRight(anguloCanhao);

            // Regras para decidir disparo
            boolean miraAlinhada = Math.abs(anguloCanhao) < 10;
            boolean distanciaOk = distanciaAlvo <= DISTANCIA_MAXIMA_TIRO;
            boolean energiaOk = getEnergy() > 0.5;
            boolean economizarEnergia = getEnergy() < 45;

            if (miraAlinhada && distanciaOk && energiaOk) {
                double potencia = economizarEnergia ? 1.0 : Math.min(3.0, getEnergy());

                out.println("🔫 Disparando com potência " + potencia);
                setColors(Color.red, Color.orange, Color.yellow); // muda cor ao atirar

                // Previsão da posição futura do inimigo
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
        // Recuar ao colidir com outro robô
        setBack(50);
        setTurnRight(45);
        execute();
    }

    public void onRobotDeath(RobotDeathEvent e) {
        // Retorna para modo busca caso inimigo morra
        if (e.getName().equals(alvoAtual)) {
            buscarAlvo();
        }
    }

    public void onHitWall(HitWallEvent e) {
        // Ação ao bater na parede
        evitandoParede = true;
        setBack(80);
        setTurnRight(90);
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // Mensagem e cor ao ser atingido
        out.println("💥 Fui atingido! Iniciando manobra evasiva...");
        setColors(Color.blue, Color.cyan, Color.lightGray);

        // Movimento evasivo com zigue-zague
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
        // Mensagem e cor ao vencer
        out.println("🏆 Vitória! O campo de batalha é meu.");
        setColors(Color.green, Color.white, Color.magenta);

        // Dança da vitória com rotação sincronizada
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
            g.drawOval((int) getX() - 250, (int) getY() - 250, 500, 500); // círculo de alcance
        }
    }

    private void buscarAlvo() {
        // Reinicia o radar para busca
        travarRadar = false;
        alvoAtual = null;
        distanciaAlvo = Double.MAX_VALUE;
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    private void evitarParedes() {
        // Detecta proximidade com as bordas e ajusta rota
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
