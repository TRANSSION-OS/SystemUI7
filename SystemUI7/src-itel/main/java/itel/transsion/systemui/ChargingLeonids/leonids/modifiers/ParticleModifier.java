package itel.transsion.systemui.ChargingLeonids.leonids.modifiers;


import itel.transsion.systemui.ChargingLeonids.leonids.Particle;

public interface ParticleModifier {

	/**
	 * modifies the specific value of a particle given the current miliseconds
	 * @param particle
	 * @param miliseconds
	 */
	void apply(Particle particle, long miliseconds);

}
