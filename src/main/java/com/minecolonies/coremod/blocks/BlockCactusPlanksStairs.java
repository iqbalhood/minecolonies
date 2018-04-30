package com.minecolonies.coremod.blocks;


import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.coremod.creativetab.ModCreativeTabs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

import java.util.Locale;

public class BlockCactusPlanksStairs extends AbstractBlockMinecoloniesStairs <BlockCactusPlanksStairs> {

    /**
     * Block's Hardness
     */
    private static final float BLOCK_HARDNESS = 3F;

    /**
     * Block's Resistance
     */
    private static final float BLOCK_RESISTANCE = 1F;

    /**
     * Prefix for that block
     */
    private static final String BLOCK_NAME = "blockcactusstairs";

    /**
     * Constructor for this block
     */
    protected BlockCactusPlanksStairs()
    {
        super();
        init(name);
    }

    /**
     * Initialize the block
     */
    private void init(final String name)
    {
        setRegistryName(name);
        setUnlocalizedName(String.format("%s.%s", Constants.MOD_ID.toLowerCase(Locale.US), name));
        setCreativeTab(ModCreativeTabs.MINECOLONIES);
        setHardness(BLOCK_HARDNESS);
        setResistance(BLOCK_RESISTANCE);
        this.useNeighborBrightness = true;
    }
}
