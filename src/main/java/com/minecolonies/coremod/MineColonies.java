package com.minecolonies.coremod;

import com.minecolonies.api.configuration.Configurations;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.coremod.achievements.ModAchievements;
import com.minecolonies.coremod.colony.BarbarianSpawnEventHandler;
import com.minecolonies.coremod.colony.requestsystem.init.RequestSystemInitializer;
import com.minecolonies.coremod.colony.requestsystem.init.StandardFactoryControllerInitializer;
import com.minecolonies.coremod.commands.CommandEntryPoint;
import com.minecolonies.coremod.event.EventHandler;
import com.minecolonies.coremod.event.FMLEventHandler;
import com.minecolonies.coremod.network.messages.*;
import com.minecolonies.coremod.proxy.IProxy;
import com.minecolonies.coremod.util.RecipeHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber
@Mod(modid = Constants.MOD_ID, name = Constants.MOD_NAME, version = Constants.VERSION,
  /*dependencies = Constants.FORGE_VERSION,*/ acceptedMinecraftVersions = Constants.MC_VERSION)
public class MineColonies
{
    private static final Logger logger = LogManager.getLogger(Constants.MOD_ID);
    /**
     * Forge created instance of the Mod.
     */
    @Mod.Instance(Constants.MOD_ID)
    public static MineColonies instance;
    /**
     * Access to the proxy associated with your current side. Variable updated
     * by forge.
     */
    @SidedProxy(clientSide = Constants.CLIENT_PROXY_LOCATION, serverSide = Constants.SERVER_PROXY_LOCATION)

    public static IProxy       proxy;

    private static SimpleNetworkWrapper network;

    /**
     * Reads file log4jConfigFile as an xml configuration file.
     * Adds Appenders and non-root loggers to the existing logging Configuration.
     * Root logger is ignored.
     * Currently does not support Filters or properties on Loggers.
     * 
     * @param log4jConfigFile file name to read.
     */
    private static void updateLog4jConfiguration(final String log4jConfigFile)
    {
        try
        {
            final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            final org.apache.logging.log4j.core.config.Configuration configuration = loggerContext.getConfiguration();
            LoggerConfig parentLoggerConfig = configuration.getRootLogger();

            final ConfigurationSource localConfigurationSource = new ConfigurationSource(new FileInputStream(log4jConfigFile), new File(log4jConfigFile));
            final XmlConfiguration localXmlConfiguration = new XmlConfiguration(null, localConfigurationSource);

            localXmlConfiguration.initialize();
            localXmlConfiguration.setup();
            localXmlConfiguration.start();

            final Map<String, Appender> localAppenders = localXmlConfiguration.getAppenders();
            final Map<String, LoggerConfig> localLoggers = localXmlConfiguration.getLoggers();
            final Collection<Appender> localAppenderList = localAppenders.values();
            for (final Appender appender : localAppenderList)
            {
                configuration.addAppender(appender);
            }

            LoggerConfig localRootLoggerConfig = null;
            final List<LoggerConfig> newLoggerConfigList = new ArrayList<>();
            
            for (final LoggerConfig localFileProvidedLoggerConfig : localLoggers.values())
            {
                final List<AppenderRef> appenderRefsList = localFileProvidedLoggerConfig.getAppenderRefs();
                final AppenderRef[] appenderRefsArray;
                if (null != appenderRefsList)
                {
                    appenderRefsArray = appenderRefsList.toArray(new AppenderRef[appenderRefsList.size()]);
                }
                else
                {
                    appenderRefsArray = new AppenderRef[0];
                }
                final List<Property> propertyList = localFileProvidedLoggerConfig.getPropertyList();
                final Property[] propertyArray;
                if (null != propertyList)
                {
                    propertyArray = propertyList.toArray(new Property[propertyList.size()]);
                }
                else
                {
                    propertyArray = new Property[0];
                }
                final LoggerConfig newLoggerConfig = LoggerConfig.createLogger(localFileProvidedLoggerConfig.isAdditive(),
                        localFileProvidedLoggerConfig.getLevel(),
                        localFileProvidedLoggerConfig.getName(),
                        String.valueOf(localFileProvidedLoggerConfig.isIncludeLocation()),
                        appenderRefsArray, propertyArray,
                        configuration,
                        localFileProvidedLoggerConfig.getFilter());

                for (final AppenderRef appenderRef : appenderRefsList)
                {
                    final Appender appender = localAppenders.get(appenderRef.getRef());
                    if (null != appender)
                    {
                        newLoggerConfig.addAppender(appender, null, null);
                    }
                    else
                    {
                        // TODO: handle logger missing appender configuration error
                    }
                }

                if (newLoggerConfig.getName().isEmpty())
                {
                    if (null != localRootLoggerConfig)
                    {
                        // TODO: handle multiple root loggers configuration error
                    }
                    localRootLoggerConfig = newLoggerConfig;
                }
                
                newLoggerConfig.setLevel(newLoggerConfig.getLevel());
                newLoggerConfigList.add(newLoggerConfig);
            }

//            if (null != localRootLoggerConfig)
//            {
//                localRootLoggerConfig.setParent(parentLoggerConfig);
//                parentLoggerConfig = localRootLoggerConfig;
//                configuration.addLogger(localRootLoggerConfig.getName(), localRootLoggerConfig);
//            }

            for (LoggerConfig newLoggerConfig : newLoggerConfigList)
            {
                newLoggerConfig.setParent(parentLoggerConfig);
                configuration.addLogger(newLoggerConfig.getName(), newLoggerConfig);
            }

            loggerContext.updateLoggers();
        }
        catch (final Exception e)
        {
            // TODO: handle configuration error
        }
    }

    static
    {
        // Reconfigure logging
        org.apache.logging.log4j.LogManager.getLogger().info("Installing Minecolonies Logger.  'ERROR No logging configuration' error below this line is expected and should be ignored.");

        updateLog4jConfiguration("log4j2-minecolonies.xml");

        MinecraftForge.EVENT_BUS.register(new BarbarianSpawnEventHandler());
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new FMLEventHandler());
    }

    /**
     * Returns whether the side is client or not
     *
     * @return True when client, otherwise false
     */
    public static boolean isClient()
    {
        return proxy.isClient() && FMLCommonHandler.instance().getEffectiveSide().isClient();
    }

    /**
     * Returns whether the side is client or not
     *
     * @return True when server, otherwise false
     */
    public static boolean isServer()
    {
        return !proxy.isClient() && FMLCommonHandler.instance().getEffectiveSide().isServer();
    }

    /**
     * Getter for the minecolonies Logger.
     *
     * @return the logger.
     */
    public static Logger getLogger()
    {
        return logger;
    }

    /**
     * Event handler for forge pre init event.
     *
     * @param event the forge pre init event.
     */
    @Mod.EventHandler
    public void preInit(@NotNull final FMLPreInitializationEvent event)
    {
        StandardFactoryControllerInitializer.onPreInit();
        proxy.registerEntities();
        proxy.registerEntityRendering();
        proxy.registerEvents();

        @NotNull final Configuration configuration = new Configuration(event.getSuggestedConfigurationFile());
        configuration.load();

        if (configuration.hasChanged())
        {
            configuration.save();
        }
    }

    /**
     * Event handler for forge init event.
     *
     * @param event the forge init event.
     */
    @Mod.EventHandler
    public void init(final FMLInitializationEvent event)
    {
        initializeNetwork();

        proxy.registerTileEntities();

        proxy.registerEvents();

        proxy.registerTileEntityRendering();

        proxy.registerRenderer();

        ModAchievements.init();

        RecipeHandler.init(Configurations.gameplay.enableInDevelopmentFeatures, Configurations.gameplay.supplyChests);
    }

    private static synchronized void initializeNetwork()
    {
        int id = 0;
        network = NetworkRegistry.INSTANCE.newSimpleChannel(Constants.MOD_NAME);

        getNetwork().registerMessage(ServerUUIDMessage.class, ServerUUIDMessage.class, ++id, Side.CLIENT);

        //  ColonyView messages
        getNetwork().registerMessage(ColonyViewMessage.class, ColonyViewMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyViewCitizenViewMessage.class, ColonyViewCitizenViewMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyViewRemoveCitizenMessage.class, ColonyViewRemoveCitizenMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyViewBuildingViewMessage.class, ColonyViewBuildingViewMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyViewRemoveBuildingMessage.class, ColonyViewRemoveBuildingMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(PermissionsMessage.View.class, PermissionsMessage.View.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyStylesMessage.class, ColonyStylesMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyViewWorkOrderMessage.class, ColonyViewWorkOrderMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(ColonyViewRemoveWorkOrderMessage.class, ColonyViewRemoveWorkOrderMessage.class, ++id, Side.CLIENT);

        //  Permission Request messages
        getNetwork().registerMessage(PermissionsMessage.Permission.class, PermissionsMessage.Permission.class, ++id, Side.SERVER);
        getNetwork().registerMessage(PermissionsMessage.AddPlayer.class, PermissionsMessage.AddPlayer.class, ++id, Side.SERVER);
        getNetwork().registerMessage(PermissionsMessage.RemovePlayer.class, PermissionsMessage.RemovePlayer.class, ++id, Side.SERVER);
        getNetwork().registerMessage(PermissionsMessage.ChangePlayerRank.class, PermissionsMessage.ChangePlayerRank.class, ++id, Side.SERVER);
        getNetwork().registerMessage(PermissionsMessage.AddPlayerOrFakePlayer.class, PermissionsMessage.AddPlayerOrFakePlayer.class, ++id, Side.SERVER);

        //  Colony Request messages
        getNetwork().registerMessage(BuildRequestMessage.class, BuildRequestMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(OpenInventoryMessage.class, OpenInventoryMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(TownHallRenameMessage.class, TownHallRenameMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(MinerSetLevelMessage.class, MinerSetLevelMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(RecallCitizenMessage.class, RecallCitizenMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(BuildToolPlaceMessage.class, BuildToolPlaceMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(ToggleJobMessage.class, ToggleJobMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(HireFireMessage.class, HireFireMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(WorkOrderChangeMessage.class, WorkOrderChangeMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(AssignFieldMessage.class, AssignFieldMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(AssignmentModeMessage.class, AssignmentModeMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(GuardTaskMessage.class, GuardTaskMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(GuardScepterMessage.class, GuardScepterMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(RecallTownhallMessage.class, RecallTownhallMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(TransferItemsRequestMessage.class, TransferItemsRequestMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(MarkBuildingDirtyMessage.class, MarkBuildingDirtyMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(ChangeFreeToInteractBlockMessage.class, ChangeFreeToInteractBlockMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(LumberjackSaplingSelectorMessage.class, LumberjackSaplingSelectorMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(ToggleHousingMessage.class, ToggleHousingMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(AssignUnassignMessage.class, AssignUnassignMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(OpenCraftingGUIMessage.class, OpenCraftingGUIMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(AddRemoveRecipeMessage.class, AddRemoveRecipeMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(ChangeRecipePriorityMessage.class, ChangeRecipePriorityMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(UpgradeWarehouseMessage.class, UpgradeWarehouseMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(BuildToolPasteMessage.class, BuildToolPasteMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(TransferItemsToCitizenRequestMessage.class, TransferItemsToCitizenRequestMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(UpdateRequestStateMessage.class, UpdateRequestStateMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(BuildingSetStyleMessage.class, BuildingSetStyleMessage.class, ++id, Side.SERVER);

        // Schematic transfer messages
        getNetwork().registerMessage(SchematicRequestMessage.class, SchematicRequestMessage.class, ++id, Side.SERVER);
        getNetwork().registerMessage(SchematicSaveMessage.class, SchematicSaveMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(SchematicSaveMessage.class, SchematicSaveMessage.class, ++id, Side.SERVER);

        //Client side only
        getNetwork().registerMessage(BlockParticleEffectMessage.class, BlockParticleEffectMessage.class, ++id, Side.CLIENT);
        getNetwork().registerMessage(SaveScanMessage.class, SaveScanMessage.class, ++id, Side.CLIENT);

        //JEI Messages
        getNetwork().registerMessage(TransferRecipeCrafingTeachingMessage.class, TransferRecipeCrafingTeachingMessage.class, ++id, Side.SERVER);
    }

    public static SimpleNetworkWrapper getNetwork()
    {
        return network;
    }

    @Mod.EventHandler
    public void postInit(final FMLPostInitializationEvent event)
    {
        RequestSystemInitializer.onPostInit();
    }

    @Mod.EventHandler
    public void serverLoad(final FMLServerStartingEvent event)
    {
        // register server commands
        event.registerServerCommand(new CommandEntryPoint());
    }
}
