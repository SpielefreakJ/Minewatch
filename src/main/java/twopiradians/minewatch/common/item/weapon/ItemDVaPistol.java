package twopiradians.minewatch.common.item.weapon;

import java.util.HashMap;
import java.util.LinkedList;

import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import twopiradians.minewatch.client.gui.display.EntityGuiPlayer;
import twopiradians.minewatch.client.key.Keys;
import twopiradians.minewatch.client.model.ModelMWArmor;
import twopiradians.minewatch.common.CommonProxy.EnumParticle;
import twopiradians.minewatch.common.Minewatch;
import twopiradians.minewatch.common.config.Config;
import twopiradians.minewatch.common.entity.hero.EntityHero;
import twopiradians.minewatch.common.entity.projectile.EntityDVaBullet;
import twopiradians.minewatch.common.hero.Ability;
import twopiradians.minewatch.common.hero.RenderManager;
import twopiradians.minewatch.common.sound.ModSoundEvents;
import twopiradians.minewatch.common.util.EntityHelper;
import twopiradians.minewatch.common.util.Handlers;
import twopiradians.minewatch.common.util.TickHandler;
import twopiradians.minewatch.common.util.TickHandler.Handler;
import twopiradians.minewatch.common.util.TickHandler.Identifier;
import twopiradians.minewatch.packet.SPacketSimple;

public class ItemDVaPistol extends ItemMWWeapon {

	/**Entities with the last tick that their armor was rendered (used to make sure particles only rendered once per tick)*/
	private static HashMap<EntityLivingBase, Integer> tickRendered = Maps.newHashMap();

	//public static final Handler RECOLOR = new Handler(Identifier.DVA_RECOLOR, false) {};
	
	public ItemDVaPistol() {
		super(20);
		//second weapon
		this.hasOffhand = false;
	}

	@Override
	public void onItemLeftClick(ItemStack stack, World world, EntityLivingBase player, EnumHand hand) { 
		// shoot
		if (this.canUse(player, true, hand, false) && !world.isRemote) {
			EntityDVaBullet bullet = new EntityDVaBullet(player.world, player, hand.ordinal());
			//aim (-1 = speed; 0.0F = recoil)
			EntityHelper.setAim(bullet, player, player.rotationPitch, player.rotationYawHead, -1, 0.0F, hand, 10, 0.5f);
			player.world.spawnEntity(bullet);
			//sound
			ModSoundEvents.DVA_SHOOT.playSound(player, 1.0f, player.world.rand.nextFloat()/20+0.95f);
			//amo use
			this.subtractFromCurrentAmmo(player, 1);
			//time between shots
			this.setCooldown(player, 5);
			if (world.rand.nextInt(40) == 0)
				player.getHeldItem(hand).damageItem(1, player);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean preRenderArmor(EntityLivingBase entity, ModelMWArmor model) { 
		super.preRenderArmor(entity, model);

		// recolor
		/*Handler handler = TickHandler.getHandler(entity, Identifier.DVA_RECOLOR);
		if (handler != null) {
			float percent = ((float) handler.ticksLeft) / handler.initialTicks;
			GlStateManager.color((255f-172f*percent)/255f, (255f-62f*percent)/255f, (255f-38f*percent)/255f, 1);
			return true;
		}*/

		return false; 
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void preRenderGameOverlay(Pre event, EntityPlayer player, double width, double height, EnumHand hand) {
		// tracer's dash
		if (hand == EnumHand.MAIN_HAND && event.getType() == ElementType.CROSSHAIRS && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
			GlStateManager.enableBlend();

			double scale = 3d*Config.guiScale;
			GlStateManager.translate(width/2, height/2, 0);
			GlStateManager.scale(scale, scale, 1);
			Minecraft.getMinecraft().getTextureManager().bindTexture(RenderManager.ABILITY_OVERLAY);/*
			int uses = this.hero.ability2.getUses(player);
			GuiUtils.drawTexturedModalRect(-5, 8, 1, uses > 2 ? 239 : 243, 10, 4, 0);
			GlStateManager.scale(0.75f, 0.75f, 1);
			GuiUtils.drawTexturedModalRect(-5, 8, 1, uses > 1 ? 239 : 243, 10, 4, 0);
			GlStateManager.scale(0.75f, 0.75f, 1);
			GuiUtils.drawTexturedModalRect(-5, 8, 1, uses > 0 ? 239 : 243, 10, 4, 0);*/

			GlStateManager.disableBlend();
		}
	}

	public static class SavedState {

		public static final int SAVE_INTERVAL = 2;

		private static HashMap<EntityLivingBase, LinkedList<SavedState>> statesClient = Maps.newHashMap();
		private static HashMap<EntityLivingBase, LinkedList<SavedState>> statesServer = Maps.newHashMap();

		public int ticksExisted;
		public int dimensionId;
		public float health;
		public float rotationYaw;
		public float rotationPitch;
		public double posX;
		public double posY;
		public double posZ;

		private SavedState(EntityLivingBase entity) {
			// copy data to state
			this.ticksExisted = entity.ticksExisted;
			this.dimensionId = entity.world.provider.getDimension();
			this.health = entity.getHealth();
			this.rotationYaw = MathHelper.wrapDegrees(entity.rotationYaw);
			this.rotationPitch = MathHelper.wrapDegrees(entity.rotationPitch);
			this.posX = entity.posX;
			this.posY = entity.posY;
			this.posZ = entity.posZ;
		}

		public static void create(EntityLivingBase entity) {
			SavedState state = new SavedState(entity);
			// add this state to the list of states
			HashMap<EntityLivingBase, LinkedList<SavedState>> states = getStates(entity.world.isRemote);
			LinkedList<SavedState> list = states.containsKey(entity) ? states.get(entity) : new LinkedList<SavedState>();
			list.add(state);
			if (list.size() > 60 / SAVE_INTERVAL)
				list.removeFirst();
			states.put(entity, list);
		}

		/**Can this state be applied to the entity*/
		public boolean canApply(EntityLivingBase entity) {
			return this.dimensionId == entity.world.provider.getDimension() && 
					entity.ticksExisted - this.ticksExisted < 100 && 
					entity.getDistance(posX, posY, posZ) < 50;
		}

		/**Apply this entity's latest save state*/
		public static void applyState(EntityLivingBase entity) {
			HashMap<EntityLivingBase, LinkedList<SavedState>> states = getStates(entity.world.isRemote);
			SavedState state = states.containsKey(entity) ? states.get(entity).pollLast() : null;
			if (state != null && state.canApply(entity)) {
				entity.prevPosX = entity.posX;
				entity.prevPosY = entity.posY;
				entity.prevPosZ = entity.posZ;
				entity.prevRotationYaw = entity.rotationYaw;
				entity.prevRotationPitch = entity.rotationPitch;
				entity.prevRenderYawOffset = entity.renderYawOffset;
				entity.prevRotationYawHead = entity.rotationYawHead;
				entity.rotationYaw = state.rotationYaw;
				entity.rotationPitch = state.rotationPitch;
				entity.renderYawOffset = state.rotationYaw;
				entity.rotationYawHead = state.rotationYaw;
				entity.fallDistance = 0;
				if (!entity.world.isRemote)
					EntityHelper.attemptTeleport(entity, state.posX, state.posY, state.posZ);

				if (state.health > entity.getHealth())
					entity.setHealth(state.health);
			}
		}

		public static HashMap<EntityLivingBase, LinkedList<SavedState>> getStates(boolean isRemote) {
			return isRemote ? statesClient : statesServer;
		}

	}

}