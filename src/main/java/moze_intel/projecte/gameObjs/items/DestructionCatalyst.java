package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.List;

import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.ParticlePKT;
import moze_intel.projecte.utils.Coordinates;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DestructionCatalyst extends ItemCharge
{
	public DestructionCatalyst() 
	{
		super("destruction_catalyst", (byte)3);
		this.setNoRepair();
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (world.isRemote) return stack;

		MovingObjectPosition mop = this.getMovingObjectPositionFromPlayer(world, player, false);

		if (mop != null && mop.typeOfHit.equals(MovingObjectType.BLOCK))
		{
			int charge = this.getCharge(stack);
			int numRows;
			boolean hasAction = false;
			
			if (charge == 0)
			{
				numRows = 1;
			}
			else if (charge == 1)
			{
				numRows = 4;
			}
			else if (charge == 2)
			{
				numRows = 9;
			}
			else 
			{
				numRows = 16;
			}
			
			ForgeDirection direction = ForgeDirection.getOrientation(mop.sideHit);
			
			Coordinates coords = new Coordinates(mop);
			AxisAlignedBB box = getBoxFromDirection(direction, coords, numRows);
			
			List<ItemStack> drops = new ArrayList();
			
			for (int x = (int) box.minX; x <= box.maxX; x++)
				for (int y = (int) box.minY; y <= box.maxY; y++)
					for (int z = (int) box.minZ; z <= box.maxZ; z++)
					{
						Block block = world.getBlock(x, y, z);
						float hardness = block.getBlockHardness(world, x, y, z);
						
						if (block == Blocks.air || hardness >= 50.0F || hardness == -1.0F)
						{
							continue;
						}
						
						if (!consumeFuel(player, stack, 8, true))
						{
							break;
						}
						
						if (!hasAction)
						{
							hasAction = true;
						}
						
						ArrayList<ItemStack> list = WorldHelper.getBlockDrops(world, player, block, stack, x, y, z);
						
						if (list != null && list.size() > 0)
						{
							drops.addAll(list);
						}
						
						world.setBlockToAir(x, y, z);
						
						if (world.rand.nextInt(8) == 0)
						{
							PacketHandler.sendToAllAround(new ParticlePKT("largesmoke", x, y, z), new TargetPoint(world.provider.dimensionId, x, y + 1, z, 32));
						}
					}

			PlayerHelper.swingItem(((EntityPlayerMP) player));
			if (hasAction)
			{
				WorldHelper.createLootDrop(drops, world, mop.blockX, mop.blockY, mop.blockZ);
			}
		}
			
		return stack;
	}
	
	public AxisAlignedBB getBoxFromDirection(ForgeDirection direction, Coordinates coords, int charge)
	{
		charge--;
		
		if (direction.offsetX != 0)
		{
			if (direction.offsetX > 0)
				return AxisAlignedBB.getBoundingBox(coords.x - charge, coords.y - 1, coords.z - 1, coords.x, coords.y + 1, coords.z + 1);
			else return AxisAlignedBB.getBoundingBox(coords.x, coords.y - 1, coords.z - 1, coords.x + charge, coords.y + 1, coords.z + 1);
		}
		else if (direction.offsetY != 0)
		{
			if (direction.offsetY > 0)
				return AxisAlignedBB.getBoundingBox(coords.x - 1, coords.y - charge, coords.z - 1, coords.x + 1, coords.y, coords.z + 1);
			else return AxisAlignedBB.getBoundingBox(coords.x - 1, coords.y, coords.z - 1, coords.x + 1, coords.y + charge, coords.z + 1);
		}
		else
		{
			if (direction.offsetZ > 0)
				return AxisAlignedBB.getBoundingBox(coords.x - 1, coords.y - 1, coords.z - charge, coords.x + 1, coords.y + 1, coords.z);
			else return AxisAlignedBB.getBoundingBox(coords.x - 1, coords.y - 1, coords.z, coords.x + 1, coords.y + 1, coords.z + charge);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register)
	{
		this.itemIcon = register.registerIcon(this.getTexture("destruction_catalyst"));
	}
}
