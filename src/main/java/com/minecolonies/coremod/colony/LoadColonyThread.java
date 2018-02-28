package com.minecolonies.coremod.colony;

import com.minecolonies.api.configuration.Configurations;
import com.minecolonies.api.util.ChunkLoadStorage;
import com.minecolonies.api.util.Utils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.DimensionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static com.minecolonies.coremod.colony.ColonyManager.*;

public class LoadColonyThread extends Thread
{
    /**
     * The chunk x pos.
     */
    private final int chunkX;

    /**
     * The chunk z pos.
     */
    private final int chunkZ;

    /**
     * The colony id.
     */
    private final int id;

    /**
     * If its an addition or deletion.
     */
    private final boolean add;

    /**
     * The deletion.
     */
    private final int dimension;

    /**
     * The chunks which are missing to load.
     */
    private AtomicInteger missingChunksToLoad = new AtomicInteger();

    /**
     * Create an instance of this thread.
     *
     * @param chunkX the x starter.
     * @param chunkZ the z starter.
     * @param id the colony id.
     * @param dimension the dimension.
     * @param add if addition or deletion.
     */
    public LoadColonyThread(final int chunkX, final int chunkZ, final int id, final int dimension, final boolean add)
    {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.id = id;
        this.add = add;
        this.dimension = dimension;
    }

    @Override
    public void run()
    {
        final int range = Configurations.gameplay.workingRangeTownHallChunks;
        final int buffer = Configurations.gameplay.townHallPaddingChunk;

        final int maxRange = range * 2 + buffer;
        @NotNull final File chunkDir = new File(DimensionManager.getWorld(0).getSaveHandler().getWorldDirectory(), CHUNK_INFO_PATH);
        Utils.checkDirectory(chunkDir);

        for (int i = chunkX - maxRange; i <= chunkX + maxRange; i++)
        {
            for (int j = chunkZ - maxRange; j <= chunkZ + maxRange; j++)
            {
                final boolean owning = i >= chunkX - range && j >= chunkZ - range && i <= chunkX + range && j <= chunkZ + range;
                @NotNull final ChunkLoadStorage newStorage = new ChunkLoadStorage(id, ChunkPos.asLong(i, j), add, dimension, owning);
                @NotNull final File file = new File(chunkDir, String.format(FILENAME_CHUNK, i, j, dimension));
                if (file.exists())
                {
                    @Nullable final NBTTagCompound chunkData = loadNBTFromPath(file);
                    final ChunkLoadStorage storage = new ChunkLoadStorage(chunkData);
                    storage.merge(newStorage);
                    if (storage.isEmpty())
                    {
                        file.delete();
                    }
                    else
                    {
                        saveNBTToPath(file, newStorage.toNBT());
                        missingChunksToLoad.incrementAndGet();
                    }
                }
                else
                {
                    saveNBTToPath(file, newStorage.toNBT());
                    missingChunksToLoad.incrementAndGet();
                }
            }
        }
    }

    /**
     * Get a copy of the amount of missing chunks to load.
     * @return the amount and reset the existing to 0.
     */
    public int getMissingChunksToLoad()
    {
        return missingChunksToLoad.getAndSet(0);
    }
}
