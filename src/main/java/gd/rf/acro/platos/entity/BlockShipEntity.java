package gd.rf.acro.platos.entity;

import gd.rf.acro.platos.PlatosTransporters;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class BlockShipEntity extends PigEntity {
    public BlockShipEntity(EntityType<? extends PigEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean canWalkOnFluid(Fluid fluid) {
        if(this.getEquippedStack(EquipmentSlot.CHEST).getItem()==Items.OAK_PLANKS)
        {
            if(this.getEquippedStack(EquipmentSlot.CHEST).getTag().getInt("type")==0)
            {
                return FluidTags.WATER.contains(fluid);
            }
        }
        return false;
    }

    @Override
    public float getSaddledSpeed() {
        if(this.getPrimaryPassenger() instanceof  PlayerEntity)
        {
            if(((PlayerEntity) this.getPrimaryPassenger()).getMainHandStack().getItem()== PlatosTransporters.CONTROL_KEY_ITEM)
            {
                if(this.getEquippedStack(EquipmentSlot.CHEST).getItem()==Items.OAK_PLANKS)
                {
                    if(this.getEquippedStack(EquipmentSlot.CHEST).getTag().getInt("type")==0)
                    {
                        if(this.world.getBlockState(this.getBlockPos().down()).getBlock()== Blocks.WATER)
                        {
                            return 0.2f;
                        }
                        return 0.05f;
                    }
                    if(this.getEquippedStack(EquipmentSlot.CHEST).getTag().getInt("type")==1)
                    {
                        return 0f;
                    }
                    else
                    {
                        return 0.2f;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    protected void initGoals() { }

    @Override
    public boolean canBeRiddenInWater() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_BOAT_PADDLE_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BOAT_PADDLE_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BOAT_PADDLE_WATER;
    }

    @Override
    protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        return 0;
    }

    public void setModel(ListTag input, int direction, int offset, int type, CompoundTag storage)
    {
        ItemStack itemStack = new ItemStack(Items.OAK_PLANKS);
        CompoundTag tag = new CompoundTag();
        tag.putString("model", UUID.randomUUID().toString());
        tag.put("parts",input);
        tag.putInt("direction",direction);
        tag.putInt("offset",offset);
        tag.putInt("type",type);
        tag.put("storage",storage);
        itemStack.setTag(tag);
        this.equipStack(EquipmentSlot.CHEST,itemStack);
    }

    public void tryDisassemble()
    {
        if(this.getEquippedStack(EquipmentSlot.CHEST).getItem()==Items.OAK_PLANKS)
        {
            ListTag list = (ListTag) this.getEquippedStack(EquipmentSlot.CHEST).getTag().get("parts");
            int offset = this.getEquippedStack(EquipmentSlot.CHEST).getTag().getInt("offset");
            CompoundTag storage = this.getEquippedStack(EquipmentSlot.CHEST).getTag().getCompound("storage");
            for (Tag tag : list)
            {
                String[] split = tag.asString().split(" ");
                if (world.getBlockState(this.getBlockPos().add(Integer.parseInt(split[1]), Integer.parseInt(split[2]) + offset, Integer.parseInt(split[3]))).getBlock() != Blocks.AIR) {
                    if (this.getPrimaryPassenger() instanceof PlayerEntity) {
                        ((PlayerEntity) this.getPrimaryPassenger()).sendMessage(new LiteralText("cannot disassemble, not enough space"), false);
                    }
                    return;
                }
            }
            list.forEach(block->
            {
                String[] split = block.asString().split(" ");
                world.setBlockState(this.getBlockPos().add(Integer.parseInt(split[1]),Integer.parseInt(split[2])+offset,Integer.parseInt(split[3])), Block.getStateFromRawId(Integer.parseInt(split[0])));
            });
            storage.getKeys().forEach(blockEntity->
            {
                String[] split = blockEntity.split(" ");
                BlockPos newpos = this.getBlockPos().add(Integer.parseInt(split[0]),Integer.parseInt(split[1])+offset,Integer.parseInt(split[2]));
                BlockEntity entity = world.getBlockEntity(newpos);
                CompoundTag data = storage.getCompound(blockEntity);
                if(data!=null)
                {
                    data.putInt("x", newpos.getX());
                    data.putInt("y", newpos.getY());
                    data.putInt("z", newpos.getZ());
                    entity.fromTag(world.getBlockState(newpos), data);
                    entity.markDirty();
                }
            });
            this.removeAllPassengers();
            this.teleport(0,-1000,0);
        }
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected boolean movesIndependently() {
        return false;
    }

    @Override
    public boolean canMoveVoluntarily() {
        return false;
    }

    @Override
    public boolean canBeControlledByRider() {
        return true;
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        if(!player.getEntityWorld().isClient && player.getStackInHand(hand)==ItemStack.EMPTY && hand==Hand.MAIN_HAND)
        {
            player.startRiding(this,true);
            return ActionResult.SUCCESS;
        }
        if(!player.getEntityWorld().isClient && player.getStackInHand(hand).getItem()==PlatosTransporters.LIFT_JACK_ITEM && hand==Hand.MAIN_HAND)
        {
            this.getEquippedStack(EquipmentSlot.CHEST).getTag().putInt("offset",player.getStackInHand(hand).getTag().getInt("off"));
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void updatePassengerPosition(Entity passenger) {
        int extra = 0;
        if(this.getEquippedStack(EquipmentSlot.CHEST).getItem()==Items.OAK_PLANKS)
        {
            extra = this.getEquippedStack(EquipmentSlot.CHEST).getTag().getInt("offset")-1;
        }
        if(this.getPrimaryPassenger() instanceof PlayerEntity)
        {
            if(passenger==this.getPrimaryPassenger())
            {
                passenger.updatePosition(this.getX(),this.getY()+0.5+extra,this.getZ());
            }
            else
            {
                Vector3f dir = this.getMovementDirection().getOpposite().getUnitVector();
                int vv = this.getPassengerList().indexOf(passenger);
                passenger.updatePosition(this.getPrimaryPassenger().getX()+dir.getX()*vv, this.getPrimaryPassenger().getY()+dir.getY()*vv, this.getPrimaryPassenger().getZ()+dir.getZ()*vv);
            }
        }
    }


}
