package master_t;

import robocode.*;
import java.awt.Color;
import static robocode.util.Utils.normalRelativeAngleDegrees;

public class Nabucodonosor extends AdvancedRobot {

    private static final double DISTANCIA_MAXIMA_TIRO = 500;
    private static final double MARGEM = 60;

    private String alvoAtual = null;
    private double distanciaAlvo = Double.MAX_VALUE;
    private long ultimoScan = 0;
    private boolean zigZag = false;

    private double larguraArena;
    private double alturaArena;

    public void run() {
        setColors(Color.black, Color.red, Color.orange);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        larguraArena = getBattleFieldWidth();
        alturaArena = getBattleFieldHeight();

        buscarAlvo();

        while (true) {
            controlarRadar();
            controlarMovimento();
            evitarParedes();
            execute();
        }
    }

    private void controlarRadar() {
        if (alvoAtual == null || (getTime() - ultimoScan > 40)) {
            buscarAlvo();
        }
    }

    private void controlarMovimento() {
        if (alvoAtual != null) {
            double angulo = normalRelativeAngleDegrees(90 + (distanciaAlvo / 15));
            setTurnRight(zigZag ? angulo : -angulo);
            setAhead(150);
            zigZag = !zigZag;
        } else {
            setTurnRight(30);
            setAhead(100);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (alvoAtual == null || e.getName().equals(alvoAtual) || e.getDistance() < distanciaAlvo) {
            alvoAtual = e.getName();
            distanciaAlvo = e.getDistance();
            ultimoScan = getTime();

            double anguloAbsoluto = getHeading() + e.getBearing();
            double anguloCanhao = normalRelativeAngleDegrees(anguloAbsoluto - getGunHeading());
            double anguloRadar = normalRelativeAngleDegrees(anguloAbsoluto - getRadarHeading());

            setTurnRadarRight(anguloRadar);
            setTurnGunRight(anguloCanhao);

            if (Math.abs(anguloCanhao) < 10 && distanciaAlvo <= DISTANCIA_MAXIMA_TIRO) {
                double potencia = calcularPotenciaTiro();
                dispararComPrevisao(e, potencia);
            }
        }
    }

    private double calcularPotenciaTiro() {
        return getEnergy() < 45 ? 1.0 : Math.min(3.0, getEnergy());
    }

    private void dispararComPrevisao(ScannedRobotEvent e, double potencia) {
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

    private void buscarAlvo() {
        alvoAtual = null;
        distanciaAlvo = Double.MAX_VALUE;
        setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    private void evitarParedes() {
        double ajuste = 0;

        if (getX() < MARGEM) ajuste = 90;
        if (getX() > larguraArena - MARGEM) ajuste = 270;
        if (getY() < MARGEM) ajuste = 0;
        if (getY() > alturaArena - MARGEM) ajuste = 180;

        if (ajuste != 0) {
            setTurnRight(normalRelativeAngleDegrees(ajuste - getHeading()));
            setAhead(80);
        }
    }

    public void onHitRobot(HitRobotEvent e) {
        setBack(50);
        setTurnRight(45);
    }

    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(alvoAtual)) {
            buscarAlvo();
        }
    }

    public void onHitWall(HitWallEvent e) {
        setBack(80);
        setTurnRight(90);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        zigZag = !zigZag;
        if (getEnergy() > 20) {
            setBack(30);
            setTurnRight(zigZag ? 45 : -45);
        } else {
            setAhead(40);
            setTurnLeft(zigZag ? 60 : -60);
        }
    }
}
