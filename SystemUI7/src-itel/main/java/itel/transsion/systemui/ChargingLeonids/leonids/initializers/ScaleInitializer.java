package itel.transsion.systemui.ChargingLeonids.leonids.initializers;

import java.util.Random;

import itel.transsion.systemui.ChargingLeonids.leonids.Particle;


public class ScaleInitializer implements ParticleInitializer {

	private float mMaxScale;
	private float mMinScale;

	public ScaleInitializer(float minScale, float maxScale) {
		mMinScale = minScale;
		mMaxScale = maxScale;
	}

	@Override
	public void initParticle(Particle p, Random r) {
		float scale = r.nextFloat()*(mMaxScale-mMinScale) + mMinScale;
		p.mScale = scale;
	}

}
