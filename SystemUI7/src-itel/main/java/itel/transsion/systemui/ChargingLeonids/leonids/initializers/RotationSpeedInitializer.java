package itel.transsion.systemui.ChargingLeonids.leonids.initializers;

import java.util.Random;

import itel.transsion.systemui.ChargingLeonids.leonids.Particle;

public class RotationSpeedInitializer implements ParticleInitializer {

	private float mMinRotationSpeed;
	private float mMaxRotationSpeed;

	public RotationSpeedInitializer(float minRotationSpeed,	float maxRotationSpeed) {
		mMinRotationSpeed = minRotationSpeed;
		mMaxRotationSpeed = maxRotationSpeed;
	}

	@Override
	public void initParticle(Particle p, Random r) {
		float rotationSpeed = r.nextFloat()*(mMaxRotationSpeed-mMinRotationSpeed) + mMinRotationSpeed;
		p.mRotationSpeed = rotationSpeed;
	}

}
