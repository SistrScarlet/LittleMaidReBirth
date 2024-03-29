package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Sets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.util.Collection;
import java.util.Set;

public class TickTimeBaseNeedSalary implements NeedSalary {
    private final LivingEntity mob;
    private final InventorySupplier hasInventory;
    private final int maxSalary;
    private final Set<Item> salaries = Sets.newHashSet();
    private int salary;
    private int nextSalaryTicks;
    private boolean isStrike;
    private int checkInventoryCool;

    public TickTimeBaseNeedSalary(LivingEntity mob, InventorySupplier hasInventory, int maxSalary, Collection<Item> salaries) {
        this.mob = mob;
        this.salaries.addAll(salaries);
        this.maxSalary = maxSalary;
        this.hasInventory = hasInventory;
    }

    public void tick() {
        //クライアント側かストライキの場合処理しない
        if (mob.world.isRemote || isStrike) {
            return;
        }
        //消費部分
        if (--nextSalaryTicks <= 0) {
            this.nextSalaryTicks = 24000;
            if (!consumeSalary(1)) {
                this.isStrike = true;
            }
        }
        //マックスの場合は補充しない
        if (maxSalary <= salary) {
            return;
        }
        //補充部分
        if (0 < --checkInventoryCool) {
            return;
        }
        checkInventoryCool = 200;
        IInventory inventory = hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isSalary(stack)) {
                while (true) {
                    if (receiveSalary(1)) {
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            //給料アイテムがなくなったら次のアイテムへ
                            break;
                        }
                    } else {
                        //給料を受け取れなくなったらreturn
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean receiveSalary(int num) {
        if (maxSalary < salary + num) {
            salary = maxSalary;
            return false;
        }
        salary += num;
        return true;
    }

    @Override
    public boolean consumeSalary(int num) {
        if (salary < num) {
            salary = 0;
            return false;
        }
        salary -= num;
        return true;
    }

    @Override
    public int getSalary() {
        return salary;
    }

    @Override
    public boolean isSalary(ItemStack stack) {
        return salaries.contains(stack.getItem());
    }

    @Override
    public boolean isStrike() {
        return isStrike;
    }

    @Override
    public void setStrike(boolean strike) {
        this.isStrike = strike;
    }

    public void writeSalary(CompoundNBT nbt) {
        nbt.putInt("salary", salary);
        nbt.putInt("nextSalaryTicks", nextSalaryTicks);
        nbt.putBoolean("isStrike", isStrike);
    }

    public void readSalary(CompoundNBT nbt) {
        salary = nbt.getInt("salary");
        nextSalaryTicks = nbt.getInt("nextSalaryTicks");
        isStrike = nbt.getBoolean("isStrike");
    }
}
