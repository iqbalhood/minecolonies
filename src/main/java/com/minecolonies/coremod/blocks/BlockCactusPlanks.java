package com.minecolonies.coremod.blocks;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.coremod.creativetab.ModCreativeTabs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;


import static com.minecolonies.api.util.constant.Suppression.DEPRECATION;

public class BlockCactusPlanks extends AbstractBlockMinecolonies<BlockCactusPlanks>
{
    /**
     *Block name
     */
    public static final String BLOCK_NAME = "blockCactusPlanks";

    /**
     * Hardness for planks
     */
    public static final float BLOCK_HARDNESS = 3F;

    /**
     * Resistance for planks
     */
    public static final float RESISTANCE = 1F;


    /**
     * Constructor for block
     */
    public BlockCactusPlanks()
    {
        super(Material.WOOD);
        initBlock();
    }

    /**
     * Set name, resistance, tab in creative mode and hardness
     */
    private void initBlock()
    {
        setRegistryName(BLOCK_NAME);
        setUnlocalizedName(String.format("%s.%s", Constants.MOD_ID.toLowerCase(),BLOCK_NAME));
        setResistance(RESISTANCE);
        setCreativeTab(ModCreativeTabs.MINECOLONIES);
        setHardness(BLOCK_HARDNESS);
    }
}
