package moze_intel.projecte.gameObjs.items.tools;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moze_intel.projecte.gameObjs.items.ItemMode;
import moze_intel.projecte.utils.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public abstract class PEToolBase extends ItemMode
{
	protected String pePrimaryToolClass;
	protected String peToolMaterial;
	protected Set<Material> harvestMaterials;
	protected Set<String> secondaryClasses;

	public PEToolBase(String unlocalName, byte numCharge, String[] modeDescrp)
	{
		super(unlocalName, numCharge, modeDescrp);
		harvestMaterials = Sets.newHashSet();
		secondaryClasses = Sets.newHashSet();
	}

	@Override
	public boolean canHarvestBlock(Block block, ItemStack stack)
	{
		return harvestMaterials.contains(block.getMaterial());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isFull3D()
	{
		return true;
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass)
	{
		if (this.pePrimaryToolClass.equals(toolClass) || this.secondaryClasses.contains(toolClass))
		{
			return 4; // TiCon
		}
		return -1;
	}

	@Override
	public float getDigSpeed(ItemStack stack, Block block, int metadata)
	{
		if ("dm_tools".equals(this.peToolMaterial))
		{
			if (canHarvestBlock(block, stack) || ForgeHooks.canToolHarvestBlock(block, metadata, stack))
			{
				return 14.0f + (12.0f * this.getCharge(stack));
			}
		}
		else if ("rm_tools".equals(this.peToolMaterial))
		{
			if (canHarvestBlock(block, stack) || ForgeHooks.canToolHarvestBlock(block, metadata, stack))
			{
				return 16.0f + (14.0f * this.getCharge(stack));
			}
		}
		return 1.0F;
	}

	@Override
	public void registerIcons(IIconRegister register)
	{
		this.itemIcon = register.registerIcon(this.getTexture(peToolMaterial, pePrimaryToolClass));
	}

	/**
	 * Deforests in an AOE. Charge affects the AOE.
	 */
	protected void deforestAOE(World world, ItemStack stack, EntityPlayer player)
	{
		byte charge = getCharge(stack);
		if (charge == 0 || world.isRemote)
		{
			return;
		}

		List<ItemStack> drops = new ArrayList<ItemStack>();

		for (int x = (int) player.posX - (5 * charge); x <= player.posX + (5 * charge); x++)
			for (int y = (int) player.posY - (10 * charge); y <= player.posY + (10 * charge); y++)
				for (int z = (int) player.posZ - (5 * charge); z <= player.posZ + (5 * charge); z++)
				{
					Block block = world.getBlock(x, y, z);

					if (block == Blocks.air)
					{
						continue;
					}

					ItemStack s = new ItemStack(block);
					int[] oreIds = OreDictionary.getOreIDs(s);

					if (oreIds.length == 0)
					{
						continue;
					}

					String oreName = OreDictionary.getOreName(oreIds[0]);

					if (oreName.equals("logWood") || oreName.equals("treeLeaves"))
					{
						ArrayList<ItemStack> blockDrops = WorldHelper.getBlockDrops(world, player, block, stack, x, y, z);

						if (!blockDrops.isEmpty())
						{
							drops.addAll(blockDrops);
						}

						world.setBlockToAir(x, y, z);
					}
				}

		WorldHelper.createLootDrop(drops, world, player.posX, player.posY, player.posZ);
		PlayerHelper.swingItem(((EntityPlayerMP) player));
	}

	/**
	 * Gets an AABB for AOE digging operations. The charge increases both the breadth and depth of the box.
	 */
	public static AxisAlignedBB getBroadDeepBox(Coordinates coords, ForgeDirection direction, int charge)
	{
		if (direction.offsetX > 0)
		{
			return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y - charge, coords.z - charge, coords.x, coords.y + charge, coords.z + charge);
		}
		else if (direction.offsetX < 0)
		{
			return AxisAlignedBB.getBoundingBox(coords.x, coords.y - charge, coords.z - charge, coords.x + charge, coords.y + charge, coords.z + charge);
		}
		else if (direction.offsetY > 0)
		{
			return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y - charge, coords.z - charge, coords.x + charge, coords.y, coords.z + charge);
		}
		else if (direction.offsetY < 0)
		{
			return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y, coords.z - charge, coords.x + charge, coords.y + charge, coords.z + charge);
		}
		else if (direction.offsetZ > 0)
		{
			return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y - charge, coords.z - charge, coords.x + charge, coords.y + charge, coords.z);
		}
		else if (direction.offsetZ < 0)
		{
			return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y - charge, coords.z, coords.x + charge, coords.y + charge, coords.z + charge);
		}
		return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
	}

	/**
	 * Gets an AABB for AOE digging operations. The charge increases only the breadth of the box. Y level remains constant.
	 */
	public static AxisAlignedBB getFlatYBox(Coordinates coords, ForgeDirection direction, int charge)
	{
		return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y, coords.z - charge, coords.x + charge, coords.y, coords.z + charge);
	}

	/**
	 * Tills in an AOE. Charge affects the AOE.
	 */
	protected void tillAOE(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int par7)
	{
		if (!player.canPlayerEdit(x, y, z, par7, stack))
		{
			return;
		}
		else
		{
			UseHoeEvent event = new UseHoeEvent(player, stack, world, x, y, z);
			if (MinecraftForge.EVENT_BUS.post(event))
			{
				return;
			}

			if (event.getResult() == Event.Result.ALLOW)
			{
				return;
			}

			byte charge = this.getCharge(stack);
			boolean hasAction = false;
			boolean hasSoundPlayed = false;

			for (int i = x - charge; i <= x + charge; i++)
				for (int j = z - charge; j <= z + charge; j++)
				{
					Block block = world.getBlock(i, y, j);

					if (world.getBlock(i, y + 1, j).isAir(world, i, y + 1, j) && (block == Blocks.grass || block == Blocks.dirt))
					{
						Block block1 = Blocks.farmland;

						if (!hasSoundPlayed)
						{
							world.playSoundEffect((double)((float)i + 0.5F), (double)((float)y + 0.5F), (double)((float)j + 0.5F), block1.stepSound.getStepResourcePath(), (block1.stepSound.getVolume() + 1.0F) / 2.0F, block1.stepSound.getPitch() * 0.8F);
							hasSoundPlayed = true;
						}

						if (world.isRemote)
						{
							return;
						}
						else
						{
							world.setBlock(i, y, j, block1);

							if (!hasAction)
							{
								hasAction = true;
							}
						}
					}
				}
		}
	}

	/**
	 * Called by multiple tools' left click function. Charge has no effect.
	 */
	protected void digBasedOnMode(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase living)
	{
		if (world.isRemote || !(living instanceof EntityPlayer))
		{
			return;
		}

		EntityPlayer player = (EntityPlayer) living;
		byte mode = this.getMode(stack);

		if (mode == 0) // Standard
		{
			return;
		}

		MovingObjectPosition mop = this.getMovingObjectPositionFromPlayer(world, player, false);
		AxisAlignedBB box;

		if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
		{
			return;
		}

		ForgeDirection direction = ForgeDirection.getOrientation(mop.sideHit);

		if (mode == 1) // 3x Tallshot
		{
			box = AxisAlignedBB.getBoundingBox(x, y - 1, z, x, y + 1, z);
		}
		else if (mode == 2) // 3x Wideshot
		{
			if (direction.offsetX != 0)
			{
				box = AxisAlignedBB.getBoundingBox(x, y, z - 1, x, y, z + 1);
			}
			else if (direction.offsetZ != 0)
			{
				box = AxisAlignedBB.getBoundingBox(x - 1, y, z, x + 1, y, z);
			}
			else
			{
				int dir = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;

				if (dir == 0 || dir == 2)
				{
					box = AxisAlignedBB.getBoundingBox(x, y, z - 1, x, y, z + 1);
				}
				else
				{
					box = AxisAlignedBB.getBoundingBox(x - 1, y, z, x + 1, y, z);
				}
			}
		}
		else // 3x Longshot
		{
			if (direction.offsetX == 1)
			{
				box = AxisAlignedBB.getBoundingBox(x - 2, y, z, x, y, z);
			}
			else if (direction.offsetX == - 1)
			{
				box = AxisAlignedBB.getBoundingBox(x, y, z, x + 2, y, z);
			}
			else if (direction.offsetZ == 1)
			{
				box = AxisAlignedBB.getBoundingBox(x, y, z - 2, x, y, z);
			}
			else if (direction.offsetZ == -1)
			{
				box = AxisAlignedBB.getBoundingBox(x, y, z, x, y, z + 2);
			}
			else if (direction.offsetY == 1)
			{
				box = AxisAlignedBB.getBoundingBox(x, y - 2, z, x, y, z);
			}
			else
			{
				box = AxisAlignedBB.getBoundingBox(x, y, z, x, y + 2, z);
			}
		}

		List<ItemStack> drops = new ArrayList<>();

		for (int i = (int) box.minX; i <= box.maxX; i++)
			for (int j = (int) box.minY; j <= box.maxY; j++)
				for (int k = (int) box.minZ; k <= box.maxZ; k++)
				{
					Block b = world.getBlock(i, j, k);

					if (b != Blocks.air && b.getBlockHardness(world, i, j, k) != -1 && (canHarvestBlock(block, stack) || ForgeHooks.canToolHarvestBlock(block, world.getBlockMetadata(i, j, k), stack)))
					{
						drops.addAll(WorldHelper.getBlockDrops(world, player, b, stack, i, j, k));
						world.setBlockToAir(i, j, k);
					}
				}

		WorldHelper.createLootDrop(drops, world, x, y, z);
	}

	/**
	 * Carves in an AOE. Charge affects the breadth and/or depth of the AOE.
	 */
	protected void digAOE(ItemStack stack, World world, EntityPlayer player, boolean affectDepth)
	{
		if (world.isRemote || this.getCharge(stack) == 0)
		{
			return;
		}

		MovingObjectPosition mop = this.getMovingObjectPositionFromPlayer(world, player, false);

		if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
		{
			return;
		}

		AxisAlignedBB box = affectDepth ? getBroadDeepBox(new Coordinates(mop.blockX, mop.blockY, mop.blockZ), ForgeDirection.getOrientation(mop.sideHit), this.getCharge(stack))
				: getFlatYBox(new Coordinates(mop.blockX, mop.blockY, mop.blockZ), ForgeDirection.getOrientation(mop.sideHit), this.getCharge(stack));

		List<ItemStack> drops = new ArrayList<ItemStack>();

		for (int i = (int) box.minX; i <= box.maxX; i++)
			for (int j = (int) box.minY; j <= box.maxY; j++)
				for (int k = (int) box.minZ; k <= box.maxZ; k++)
				{
					Block b = world.getBlock(i, j, k);

					if (b != Blocks.air && b.getBlockHardness(world, i, j, k) != -1 && canHarvestBlock(b, stack))
					{
						drops.addAll(WorldHelper.getBlockDrops(world, player, b, stack, i, j, k));
						world.setBlockToAir(i, j, k);
					}
				}

		WorldHelper.createLootDrop(drops, world, mop.blockX, mop.blockY, mop.blockZ);
		PlayerHelper.swingItem(((EntityPlayerMP) player));
	}

	/**
	 * Attacks. Charge affects damage.
	 */
	protected void attackWithCharge(ItemStack stack, EntityLivingBase damaged, EntityLivingBase damager, float baseDmg)
	{
		if (!(damager instanceof EntityPlayer))
		{
			return;
		}

		DamageSource dmg = DamageSource.causePlayerDamage((EntityPlayer) damager);
		byte charge = this.getCharge(stack);
		float totalDmg = baseDmg;

		if (charge > 0)
		{
			dmg.setDamageBypassesArmor();
			totalDmg += charge;
		}

		damaged.attackEntityFrom(dmg, totalDmg);
	}

	/**
	 * Attacks in an AOE. Charge affects AOE, not damage (intentional)
	 */
	protected void attackAOE(ItemStack stack, EntityPlayer player, boolean slayAll, float damage)
	{
		byte charge = getCharge(stack);
		float factor = 2.5F * charge;
		AxisAlignedBB aabb = player.boundingBox.expand(factor, factor, factor);
		List<Entity> toAttack = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb);
		DamageSource src = DamageSource.causePlayerDamage(player);
		src.setDamageBypassesArmor();
		for (Entity entity : toAttack)
		{
			if (entity instanceof IMob)
			{
				entity.attackEntityFrom(src, damage);
			}
			else if (entity instanceof EntityLivingBase && slayAll)
			{
				entity.attackEntityFrom(src, damage);
			}
		}
		PlayerHelper.swingItem(((EntityPlayerMP) player));
	}

	/**
	 * Called when tools that act as shears start breaking a block
	 */
	protected void shearBlock(ItemStack stack, int x, int y, int z, EntityPlayer player)
	{
		if (player.worldObj.isRemote)
		{
			return;
		}

		Block block = player.worldObj.getBlock(x, y, z);

		if (block instanceof IShearable)
		{
			IShearable target = (IShearable) block;

			if (target.isShearable(stack, player.worldObj, x, y, z))
			{
				ArrayList<ItemStack> drops = target.onSheared(stack, player.worldObj, x, y, z, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack));
				Random rand = new Random();

				for(ItemStack drop : drops)
				{
					float f = 0.7F;
					double d = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					double d1 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					double d2 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					EntityItem entityitem = new EntityItem(player.worldObj, (double)x + d, (double)y + d1, (double)z + d2, drop);
					entityitem.delayBeforeCanPickup = 10;
					player.worldObj.spawnEntityInWorld(entityitem);
				}

				stack.damageItem(1, player);
				player.addStat(StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
			}
		}
	}

	/**
	 * Shears entities in an AOE. Charge affects AOE.
	 */
	protected void shearEntityAOE(ItemStack stack, EntityPlayer player)
	{
		World world = player.worldObj;
		if (!world.isRemote)
		{
			byte charge = this.getCharge(stack);

			int offset = ((int) Math.pow(2, 2 + charge));

			AxisAlignedBB bBox = player.boundingBox.expand(offset, offset / 2, offset);
			List<Entity> list = world.getEntitiesWithinAABB(IShearable.class, bBox);

			List<ItemStack> drops = new ArrayList<>();

			for (Entity ent : list)
			{
				IShearable target = (IShearable) ent;

				if (target.isShearable(stack, ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ))
				{
					ArrayList<ItemStack> entDrops = target.onSheared(stack, ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack));

					if (!entDrops.isEmpty())
					{
						for (ItemStack drop : entDrops)
						{
							drop.stackSize *= 2;
						}

						drops.addAll(entDrops);
					}
				}
				if (Math.random() < 0.01)
				{
					Entity e = EntityList.createEntityByName(EntityList.getEntityString(ent), world);
					e.copyDataFrom(ent, true);
					if (e instanceof EntitySheep)
					{
						((EntitySheep) e).setFleeceColor(MathUtils.randomIntInRange(16, 0));
					}
					if (e instanceof EntityAgeable)
					{
						((EntityAgeable) e).setGrowingAge(-24000);
					}
					world.spawnEntityInWorld(e);
				}
			}

			WorldHelper.createLootDrop(drops, world, player.posX, player.posY, player.posZ);
			PlayerHelper.swingItem(((EntityPlayerMP) player));
		}
	}

	/**
	 * Scans and harvests an ore vein. This is called already knowing the mop is pointing at an ore or gravel.
	 */
	protected void tryVeinMine(ItemStack stack, EntityPlayer player, MovingObjectPosition mop)
	{
		AxisAlignedBB aabb = getBroadDeepBox(new Coordinates(mop.blockX, mop.blockY, mop.blockZ), ForgeDirection.getOrientation(mop.sideHit), getCharge(stack));
		List<ItemStack> drops = Lists.newArrayList();

		for (int i = (int) aabb.minX; i <= aabb.maxX; i++)
		{
			for (int j = (int) aabb.minY; j <= aabb.maxY; j++)
			{
				for (int k = (int) aabb.minZ; k <= aabb.maxZ; k++)
				{
					Block b = player.worldObj.getBlock(i, j, k);
					if (b.getBlockHardness(player.worldObj, i, j, k) > -1 && canHarvestBlock(b, stack))
					{
						WorldHelper.harvestVein(player.worldObj, player, stack, new Coordinates(i, j, k), b, drops, 0);
					}
				}
			}
		}

		WorldHelper.createLootDrop(drops, player.worldObj, mop.blockX, mop.blockY, mop.blockZ);
	}
}
