package master_t;

import robocode.*;
import java.awt.Color;
import static robocode.util.Utils.normalRelativeAngleDegrees;

public class Nabucodonosor extends AdvancedRobot {

    // Variáveis de controle
    private boolean travarRadar = false; // Se o radar deve travar no inimigo atual
    private double distanciaAlvo = 0; // Distância até o inimigo atual
    private long ultimoScan = 0; // Tick do último scan
    private String alvoAtual = null; // Nome do inimigo atual
    private double larguraArena; // Largura da arena
    private double alturaArena; // Altura da arena
    private double margem = 60; // Margem de segurança da parede
    private boolean evitandoParede = false; // Flag para evasão de parede
    private final double DISTANCIA_MAXIMA_TIRO = 500; // Distância máxima para atirar
    private boolean zigZag = false; // Controle do movimento em zigue-zague

    public void run() {
        // Configurações visuais e técnicas
        setColors(Color.black, Color.red, Color.orange);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        // Detecta o tamanho da arena
        larguraArena = getBattleFieldWidth();
        alturaArena = getBattleFieldHeight();

        buscarAlvo(); // Inicia buscando inimigos

        while (true) {
            // Se o alvo não foi visto recentemente, volta para modo de busca
            if (travarRadar && (getTime() - ultimoScan > 30)) {
                buscarAlvo();
            }

            if (travarRadar) {
                // Movimento em zigue-zague ao redor do inimigo
                double angulo = normalRelativeAngleDegrees(90 + (distanciaAlvo / 15));
                if (zigZag) {
                    setTurnRight(angulo);
                } else {
                    setTurnLeft(angulo);
                }
                setAhead(100);
                zigZag = !zigZag;
            } else {
                // Movimento exploratório com base na forma da arena
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

            evitarParedes(); // Verifica e evita colisão com paredes
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // Só troca de alvo se o novo inimigo estiver mais próximo
        if (!travarRadar || e.getDistance() < distanciaAlvo) {
            travarRadar = true;
            distanciaAlvo = e.getDistance();
            ultimoScan = getTime();
            alvoAtual = e.getName();

            // Calcula ângulos para canhão e radar
            double anguloAbsoluto = getHeading() + e.getBearing();
            double anguloCanhao = normalRelativeAngleDegrees(anguloAbsoluto - getGunHeading());
            double anguloRadar = normalRelativeAngleDegrees(anguloAbsoluto - getRadarHeading());

            setTurnRadarRight(anguloRadar);
            setTurnGunRight(anguloCanhao);

            // Condições para disparo
            boolean miraAlinhada = Math.abs(anguloCanhao) < 10;
            boolean distanciaOk = distanciaAlvo <= DISTANCIA_MAXIMA_TIRO;
            boolean energiaOk = getEnergy() > 0.5;
            boolean economizarEnergia = getEnergy() < 45;

            if (miraAlinhada && distanciaOk && energiaOk) {
                // Potência do tiro depende da energia atual
                double potencia = economizarEnergia ? 1.0 : Math.min(3.0, getEnergy());

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
            }
        }
    }

    public void onHitRobot(HitRobotEvent e) {
        // Evita colisão com robôs recuando e girando
        setBack(50);
        setTurnRight(45);
        execute();
    }

    public void onRobotDeath(RobotDeathEvent e) {
        // Se o inimigo atual morreu, volta ao modo de busca
        if (e.getName().equals(alvoAtual)) {
            buscarAlvo();
        }
    }

    public void onHitWall(HitWallEvent e) {
        // Evita ficar preso na parede
        evitandoParede = true;
        setBack(80);
        setTurnRight(90);
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // Reage com zigue-zague evasivo ao ser atingido
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

    private void buscarAlvo() {
        // Retorna ao modo de busca girando o radar continuamente
        travarRadar = false;
        alvoAtual = null;
        distanciaAlvo = Double.MAX_VALUE;
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    private void evitarParedes() {
        // Detecta proximidade com as bordas e faz correção de rota
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
