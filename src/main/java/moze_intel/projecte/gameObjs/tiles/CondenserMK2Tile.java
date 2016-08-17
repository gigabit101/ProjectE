package moze_intel.projecte.gameObjs.tiles;

import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import javax.annotation.Nonnull;

public class CondenserMK2Tile extends CondenserTile
{
	private final IItemHandlerModifiable public_input = new WrappedItemHandler(getInput(), WrappedItemHandler.WriteMode.IN);
	private final IItemHandlerModifiable public_output = new WrappedItemHandler(getOutput(), WrappedItemHandler.WriteMode.OUT);
	private final CombinedInvWrapper joined = new CombinedInvWrapper(public_input, public_output);

	@Nonnull
	@Override
	public <T> T getCapability(@Nonnull Capability<T> cap, @Nonnull EnumFacing side)
	{
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
		{
			if (side == null)
			{
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(joined);
			}
			else if (side == EnumFacing.DOWN)
			{
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(public_output);
			}
			else
			{
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(public_input);
			}
		}

		return super.getCapability(cap, side);
	}

	@Override
	protected ItemStackHandler createInput()
	{
		return new StackHandler(42)
		{
			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
			{
				if (!isStackEqualToLock(stack) && EMCHelper.doesItemHaveEmc(stack))
					return super.insertItem(slot, stack, simulate);
				else return stack;
			}
		};
	}

	@Override
	protected ItemStackHandler createOutput()
	{
		return new StackHandler(42);
	}

	@Override
	protected void condense()
	{
		while (this.hasSpace() && this.getStoredEmc() >= requiredEmc)
		{
			pushStack();
			this.removeEMC(requiredEmc);
		}

		if (this.hasSpace())
		{
			for (int i = 0; i < getInput().getSlots(); i++)
			{
				ItemStack stack = getInput().getStackInSlot(i);

				if (stack == null)
				{
					continue;
				}

				this.addEMC(EMCHelper.getEmcValue(stack) * stack.stackSize);
				getInput().setStackInSlot(i, null);
				break;
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		getOutput().deserializeNBT(nbt.getCompoundTag("Output"));
	}

	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt = super.writeToNBT(nbt);
		nbt.setTag("Output", getOutput().serializeNBT());
		return nbt;
	}
}
