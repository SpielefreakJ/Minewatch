package twopiradians.minewatch.common.entity.hero;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import twopiradians.minewatch.client.key.Keys.KeyBind;
import twopiradians.minewatch.common.entity.hero.ai.EntityHeroAIAttackBase;
import twopiradians.minewatch.common.entity.hero.ai.EntityHeroAIAttackBase.MovementType;
import twopiradians.minewatch.common.hero.EnumHero;

public class EntityDVa extends EntityHero {

	public EntityDVa(World worldIn) {
		super(worldIn, EnumHero.DVA); 
	}

	@Override
	protected void initEntityAI() {
		super.initEntityAI();
		this.tasks.addTask(2, new EntityHeroAIAttackDVa(this, MovementType.STRAFING, 15));
	}

	public class EntityHeroAIAttackDVa extends EntityHeroAIAttackBase {

		public EntityHeroAIAttackDVa(EntityHero entity, MovementType type, float maxDistance) {
			super(entity, type, maxDistance);
		}

		@Override
		protected void attackTarget(EntityLivingBase target, boolean canSee, double distance) {
			super.attackTarget(target, canSee, distance);

			if (canSee && this.isFacingTarget() && distance <= Math.sqrt(this.maxAttackDistance)) {
				// normal attack
				this.entity.getDataManager().set(KeyBind.LMB.datamanager, true);
				this.entity.getDataManager().set(KeyBind.RMB.datamanager, false);
			}
			else 
				this.resetKeybinds();
		}
/*
		@Override
		public void updateTask() {
			super.updateTask();
			
			// blink to target
			if (this.strafingTime == -1 && (entity.moveForward > 0 || entity.moveStrafing > 0) &&
					--this.attackCooldown <= 0) {
				this.entity.getDataManager().set(KeyBind.RMB.datamanager, true);
				this.attackCooldown = 30;
			}
			else
				this.entity.getDataManager().set(KeyBind.RMB.datamanager, false);

		}*/

	}
	
}