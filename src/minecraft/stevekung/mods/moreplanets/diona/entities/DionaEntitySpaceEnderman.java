/*******************************************************************************
 * Copyright 2015 SteveKunG - More Planets Mod
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package stevekung.mods.moreplanets.diona.entities;

import java.util.UUID;

import micdoodle8.mods.galacticraft.api.entity.IEntityBreathable;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeInstance;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

public class DionaEntitySpaceEnderman extends EntityMob implements IEntityBreathable
{
	private static final UUID attackingSpeedBoostModifierUUID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
	private static final AttributeModifier attackingSpeedBoostModifier = new AttributeModifier(attackingSpeedBoostModifierUUID, "Attacking speed boost", 6.199999809265137D, 0).setSaved(false);
	public static boolean[] carriableBlocks = new boolean[4096];

	private int teleportDelay;
	private int stareTimer;
	private Entity lastEntityToAttack;
	private boolean isAggressive;

	public DionaEntitySpaceEnderman(World par1World)
	{
		super(par1World);
		this.setSize(0.6F, 2.9F);
		this.stepHeight = 1.0F;
	}

	@Override
	public boolean getCanSpawnHere()
	{
		return this.worldObj.difficultySetting > 0 && this.isValidLightLevel() && super.getCanSpawnHere();
	}

	@Override
	protected boolean isValidLightLevel()
	{
		int i = MathHelper.floor_double(this.posX);
		int j = MathHelper.floor_double(this.boundingBox.minY);
		int k = MathHelper.floor_double(this.posZ);

		if (this.worldObj.getSavedLightValue(EnumSkyBlock.Sky, i, j, k) > this.rand.nextInt(32))
		{
			return false;
		}
		else
		{
			int l = this.worldObj.getBlockLightValue(i, j, k);

			if (this.worldObj.isThundering())
			{
				int i1 = this.worldObj.skylightSubtracted;
				this.worldObj.skylightSubtracted = 10;
				l = this.worldObj.getBlockLightValue(i, j, k);
				this.worldObj.skylightSubtracted = i1;
			}
			return l <= this.rand.nextInt(8);
		}
	}

	@Override
	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setAttribute(40.0D);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setAttribute(0.30000001192092896D);
		this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setAttribute(7.0D);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataWatcher.addObject(16, new Byte((byte)0));
		this.dataWatcher.addObject(17, new Byte((byte)0));
		this.dataWatcher.addObject(18, new Byte((byte)0));
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeEntityToNBT(par1NBTTagCompound);
		par1NBTTagCompound.setShort("carried", (short)this.getCarried());
		par1NBTTagCompound.setShort("carriedData", (short)this.getCarryingData());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readEntityFromNBT(par1NBTTagCompound);
		this.setCarried(par1NBTTagCompound.getShort("carried"));
		this.setCarryingData(par1NBTTagCompound.getShort("carriedData"));
	}

	@Override
	protected Entity findPlayerToAttack()
	{
		EntityPlayer entityplayer = this.worldObj.getClosestVulnerablePlayerToEntity(this, 64.0D);

		if (entityplayer != null)
		{
			if (this.shouldAttackPlayer(entityplayer))
			{
				this.isAggressive = true;

				if (this.stareTimer == 0)
				{
					this.worldObj.playSoundAtEntity(entityplayer, "mob.endermen.stare", 1.0F, 1.0F);
				}

				if (this.stareTimer++ == 5)
				{
					this.stareTimer = 0;
					this.setScreaming(true);
					return entityplayer;
				}
			}
			else
			{
				this.stareTimer = 0;
			}
		}
		return null;
	}

	private boolean shouldAttackPlayer(EntityPlayer par1EntityPlayer)
	{
		ItemStack itemstack = par1EntityPlayer.inventory.armorInventory[3];

		if (itemstack != null && itemstack.itemID == Block.pumpkin.blockID)
		{
			return false;
		}
		else
		{
			Vec3 vec3 = par1EntityPlayer.getLook(1.0F).normalize();
			Vec3 vec31 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX - par1EntityPlayer.posX, this.boundingBox.minY + this.height / 2.0F - (par1EntityPlayer.posY + par1EntityPlayer.getEyeHeight()), this.posZ - par1EntityPlayer.posZ);
			double d0 = vec31.lengthVector();
			vec31 = vec31.normalize();
			double d1 = vec3.dotProduct(vec31);
			return d1 > 1.0D - 0.025D / d0 ? par1EntityPlayer.canEntityBeSeen(this) : false;
		}
	}

	@Override
	public void onLivingUpdate()
	{
		if (this.isWet())
		{
			this.attackEntityFrom(DamageSource.drown, 1.0F);
		}

		if (this.lastEntityToAttack != this.entityToAttack)
		{
			AttributeInstance attributeinstance = this.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
			attributeinstance.removeModifier(attackingSpeedBoostModifier);

			if (this.entityToAttack != null)
			{
				attributeinstance.applyModifier(attackingSpeedBoostModifier);
			}
		}

		this.lastEntityToAttack = this.entityToAttack;
		int i;

		if (!this.worldObj.isRemote && this.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"))
		{
			int j;
			int k;
			int l;

			if (this.getCarried() == 0)
			{
				if (this.rand.nextInt(20) == 0)
				{
					i = MathHelper.floor_double(this.posX - 2.0D + this.rand.nextDouble() * 4.0D);
					j = MathHelper.floor_double(this.posY + this.rand.nextDouble() * 3.0D);
					k = MathHelper.floor_double(this.posZ - 2.0D + this.rand.nextDouble() * 4.0D);
					l = this.worldObj.getBlockId(i, j, k);

					if (carriableBlocks[l])
					{
						this.setCarried(this.worldObj.getBlockId(i, j, k));
						this.setCarryingData(this.worldObj.getBlockMetadata(i, j, k));
						this.worldObj.setBlock(i, j, k, 0);
					}
				}
			}
			else if (this.rand.nextInt(2000) == 0)
			{
				i = MathHelper.floor_double(this.posX - 1.0D + this.rand.nextDouble() * 2.0D);
				j = MathHelper.floor_double(this.posY + this.rand.nextDouble() * 2.0D);
				k = MathHelper.floor_double(this.posZ - 1.0D + this.rand.nextDouble() * 2.0D);
				l = this.worldObj.getBlockId(i, j, k);
				int i1 = this.worldObj.getBlockId(i, j - 1, k);

				if (l == 0 && i1 > 0 && Block.blocksList[i1].renderAsNormalBlock())
				{
					this.worldObj.setBlock(i, j, k, this.getCarried(), this.getCarryingData(), 3);
					this.setCarried(0);
				}
			}
		}

		for (i = 0; i < 2; ++i)
		{
			this.worldObj.spawnParticle("portal", this.posX + (this.rand.nextDouble() - 0.5D) * this.width, this.posY + this.rand.nextDouble() * this.height - 0.25D, this.posZ + (this.rand.nextDouble() - 0.5D) * this.width, (this.rand.nextDouble() - 0.5D) * 2.0D, -this.rand.nextDouble(), (this.rand.nextDouble() - 0.5D) * 2.0D);
		}

		if (this.worldObj.isDaytime() && !this.worldObj.isRemote)
		{
			float f = this.getBrightness(1.0F);

			if (f > 0.5F && this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)) && this.rand.nextFloat() * 30.0F < (f - 0.4F) * 2.0F)
			{
				this.entityToAttack = null;
				this.setScreaming(false);
				this.isAggressive = false;
				this.teleportRandomly();
			}
		}

		if (this.isWet() || this.isBurning())
		{
			this.entityToAttack = null;
			this.setScreaming(false);
			this.isAggressive = false;
			this.teleportRandomly();
		}

		if (this.isScreaming() && !this.isAggressive && this.rand.nextInt(100) == 0)
		{
			this.setScreaming(false);
		}

		this.isJumping = false;

		if (this.entityToAttack != null)
		{
			this.faceEntity(this.entityToAttack, 100.0F, 100.0F);
		}

		if (!this.worldObj.isRemote && this.isEntityAlive())
		{
			if (this.entityToAttack != null)
			{
				if (this.entityToAttack instanceof EntityPlayer && this.shouldAttackPlayer((EntityPlayer)this.entityToAttack))
				{
					if (this.entityToAttack.getDistanceSqToEntity(this) < 16.0D)
					{
						this.teleportRandomly();
					}

					this.teleportDelay = 0;
				}
				else if (this.entityToAttack.getDistanceSqToEntity(this) > 256.0D && this.teleportDelay++ >= 30 && this.teleportToEntity(this.entityToAttack))
				{
					this.teleportDelay = 0;
				}
			}
			else
			{
				this.setScreaming(false);
				this.teleportDelay = 0;
			}
		}
		super.onLivingUpdate();
	}

	protected boolean teleportRandomly()
	{
		double d0 = this.posX + (this.rand.nextDouble() - 0.5D) * 64.0D;
		double d1 = this.posY + (this.rand.nextInt(64) - 32);
		double d2 = this.posZ + (this.rand.nextDouble() - 0.5D) * 64.0D;
		return this.teleportTo(d0, d1, d2);
	}

	protected boolean teleportToEntity(Entity par1Entity)
	{
		Vec3 vec3 = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX - par1Entity.posX, this.boundingBox.minY + this.height / 2.0F - par1Entity.posY + par1Entity.getEyeHeight(), this.posZ - par1Entity.posZ);
		vec3 = vec3.normalize();
		double d0 = 16.0D;
		double d1 = this.posX + (this.rand.nextDouble() - 0.5D) * 8.0D - vec3.xCoord * d0;
		double d2 = this.posY + (this.rand.nextInt(16) - 8) - vec3.yCoord * d0;
		double d3 = this.posZ + (this.rand.nextDouble() - 0.5D) * 8.0D - vec3.zCoord * d0;
		return this.teleportTo(d1, d2, d3);
	}

	protected boolean teleportTo(double par1, double par3, double par5)
	{
		EnderTeleportEvent event = new EnderTeleportEvent(this, par1, par3, par5, 0);
		if (MinecraftForge.EVENT_BUS.post(event)){
			return false;
		}

		double d3 = this.posX;
		double d4 = this.posY;
		double d5 = this.posZ;
		this.posX = event.targetX;
		this.posY = event.targetY;
		this.posZ = event.targetZ;
		boolean flag = false;
		int i = MathHelper.floor_double(this.posX);
		int j = MathHelper.floor_double(this.posY);
		int k = MathHelper.floor_double(this.posZ);
		int l;

		if (this.worldObj.blockExists(i, j, k))
		{
			boolean flag1 = false;

			while (!flag1 && j > 0)
			{
				l = this.worldObj.getBlockId(i, j - 1, k);

				if (l != 0 && Block.blocksList[l].blockMaterial.blocksMovement())
				{
					flag1 = true;
				}
				else
				{
					--this.posY;
					--j;
				}
			}

			if (flag1)
			{
				this.setPosition(this.posX, this.posY, this.posZ);

				if (this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty() && !this.worldObj.isAnyLiquid(this.boundingBox))
				{
					flag = true;
				}
			}
		}

		if (!flag)
		{
			this.setPosition(d3, d4, d5);
			return false;
		}
		else
		{
			short short1 = 128;

			for (l = 0; l < short1; ++l)
			{
				double d6 = l / (short1 - 1.0D);
				float f = (this.rand.nextFloat() - 0.5F) * 0.2F;
				float f1 = (this.rand.nextFloat() - 0.5F) * 0.2F;
				float f2 = (this.rand.nextFloat() - 0.5F) * 0.2F;
				double d7 = d3 + (this.posX - d3) * d6 + (this.rand.nextDouble() - 0.5D) * this.width * 2.0D;
				double d8 = d4 + (this.posY - d4) * d6 + this.rand.nextDouble() * this.height;
				double d9 = d5 + (this.posZ - d5) * d6 + (this.rand.nextDouble() - 0.5D) * this.width * 2.0D;
				this.worldObj.spawnParticle("portal", d7, d8, d9, f, f1, f2);
			}
			this.worldObj.playSoundEffect(d3, d4, d5, "mob.endermen.portal", 1.0F, 1.0F);
			this.playSound("mob.endermen.portal", 1.0F, 1.0F);
			return true;
		}
	}

	@Override
	protected String getLivingSound()
	{
		return this.isScreaming() ? "mob.endermen.scream" : "mob.endermen.idle";
	}

	@Override
	protected String getHurtSound()
	{
		return "mob.endermen.hit";
	}

	@Override
	protected String getDeathSound()
	{
		return "mob.endermen.death";
	}

	@Override
	protected int getDropItemId()
	{
		return Item.enderPearl.itemID;
	}

	@Override
	protected void dropFewItems(boolean par1, int par2)
	{
		int j = this.getDropItemId();

		if (j > 0)
		{
			int k = this.rand.nextInt(2 + par2);

			for (int l = 0; l < k; ++l)
			{
				this.dropItem(j, 1);
			}
		}
	}

	public void setCarried(int par1)
	{
		this.dataWatcher.updateObject(16, Byte.valueOf((byte)(par1 & 255)));
	}

	public int getCarried()
	{
		return this.dataWatcher.getWatchableObjectByte(16);
	}

	public void setCarryingData(int par1)
	{
		this.dataWatcher.updateObject(17, Byte.valueOf((byte)(par1 & 255)));
	}

	public int getCarryingData()
	{
		return this.dataWatcher.getWatchableObjectByte(17);
	}

	@Override
	public boolean attackEntityFrom(DamageSource par1DamageSource, float par2)
	{
		if (this.isEntityInvulnerable())
		{
			return false;
		}
		else
		{
			this.setScreaming(true);

			if (par1DamageSource instanceof EntityDamageSource && par1DamageSource.getEntity() instanceof EntityPlayer)
			{
				this.isAggressive = true;
			}

			if (par1DamageSource instanceof EntityDamageSourceIndirect)
			{
				this.isAggressive = false;

				for (int i = 0; i < 64; ++i)
				{
					if (this.teleportRandomly())
					{
						return true;
					}
				}

				return super.attackEntityFrom(par1DamageSource, par2);
			}
			else
			{
				return super.attackEntityFrom(par1DamageSource, par2);
			}
		}
	}

	public boolean isScreaming()
	{
		return this.dataWatcher.getWatchableObjectByte(18) > 0;
	}

	public void setScreaming(boolean par1)
	{
		this.dataWatcher.updateObject(18, Byte.valueOf((byte)(par1 ? 1 : 0)));
	}

	static
	{
		carriableBlocks[Block.grass.blockID] = true;
		carriableBlocks[Block.dirt.blockID] = true;
		carriableBlocks[Block.sand.blockID] = true;
		carriableBlocks[Block.gravel.blockID] = true;
		carriableBlocks[Block.plantYellow.blockID] = true;
		carriableBlocks[Block.plantRed.blockID] = true;
		carriableBlocks[Block.mushroomBrown.blockID] = true;
		carriableBlocks[Block.mushroomRed.blockID] = true;
		carriableBlocks[Block.tnt.blockID] = true;
		carriableBlocks[Block.cactus.blockID] = true;
		carriableBlocks[Block.blockClay.blockID] = true;
		carriableBlocks[Block.pumpkin.blockID] = true;
		carriableBlocks[Block.melon.blockID] = true;
		carriableBlocks[Block.mycelium.blockID] = true;
	}

	@Override
	public boolean canBreath()
	{
		return true;
	}
}