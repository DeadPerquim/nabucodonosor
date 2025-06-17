package master_t;
import robocode.*;
import java.awt.Color;
import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import static robocode.util.Utils.normalRelativeAngleDegrees;


import java.awt.*;


public class Nabucodonosor extends AdvancedRobot
{
  
int roboDetectado = 0;
   
	public void run() {
		

		 setColors(Color.black,Color.magenta,Color.yellow); // body,gun,radar

	
		while(true) {
			if(roboDetectado == 0){
			setAhead(100);
			setTurnRight(90);
			setTurnGunRight(360);
			execute();
			}
		}
	}


	public void onScannedRobot(ScannedRobotEvent  e) {
		
			double absoluteBearing = getHeading() + e.getBearing();
				double bearingFromGun = normalRelativeAngleDegrees(absoluteBearing - getGunHeading());
					if (Math.abs(bearingFromGun) <= 3){
						turnGunRight(bearingFromGun);
							if (getGunHeat() == 0) {
									setAhead(110);
									setTurnLeft(90);
									setTurnGunRight(2);
									setTurnGunLeft(2);
									setFireBullet(Math.min(3 - Math.abs(bearingFromGun), getEnergy() - .1));
									execute();
	}}
			else {
			turnGunRight(bearingFromGun);
		}
			if (bearingFromGun == 0) {
			scan();
			}
		}

	public void onHitByBullet(HitByBulletEvent e) {
	
		back(10);
	}
	

	public void onHitWall(HitWallEvent e) {
	
		setBack(25);
		setTurnLeft(45);
		execute();
	}	
}