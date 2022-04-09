package com.saajitaro.silence;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("silence")
public class Silence
{

	public void serverLoad(FMLServerStartingEvent event) {
		event.getCommandDispatcher().register(customCommand());
	}

	private LiteralArgumentBuilder<CommandSource> customCommand() {
		return LiteralArgumentBuilder.<CommandSource>literal("block")
				.then(Commands.literal("test_arg").executes(this::execute));
	}

	private int execute(CommandContext<CommandSource> ctx) {
		ServerWorld world = ctx.getSource().getWorld();
//		double x = ctx.getSource().getPos().getX();
//		double y = ctx.getSource().getPos().getY();
//		double z = ctx.getSource().getPos().getZ();
		Entity entity = ctx.getSource().getEntity();
		if (entity == null)
			entity = FakePlayerFactory.getMinecraft(world);
		//HashMap<String, String> cmdparams = new HashMap<>();
		ArrayList<String> cmdparams = new ArrayList();
		int[] index = {-1};
		Arrays.stream(ctx.getInput().split("\\s+")).forEach(param -> {
			if (index[0] >= 0)
				cmdparams.add(param);
			index[0]++;
		});
		String listString = String.join(" ", cmdparams);
		ServerLifecycleHooks.getCurrentServer().getPlayerList().sendMessage(new StringTextComponent(listString));;
		return 0;
	}

	
	// Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    File silence_config_path = new File(FMLPaths.GAMEDIR.get().toString() + "/config/silence/");
	File silence_config_file = new File(FMLPaths.GAMEDIR.get().toString() + "/config/silence/players.json");
	
	public void makeJSON() 
	{
		if (!silence_config_file.exists())
		{
			silence_config_path.mkdirs();
			try {
				silence_config_file.createNewFile();
				FileWriter writer = new FileWriter(silence_config_file);
				writer.write("{}");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public JSONObject getJSON() throws IOException
	{
		String players_json_text = Files.readString(silence_config_file.toPath());
		//JsonObject jsonObject = new JsonParser().parse(players_json_text).getAsJsonObject();
		JSONObject players_json = new JSONObject(players_json_text); 
		return players_json;
	}
	
	public ArrayList<String> getMutedPlayers(String uuid) throws IOException
	{
		JSONObject players_json = getJSON();
		JSONArray keys = players_json.names();
		
		for (int i = 0; i < keys.length(); i ++)
		{
			String key = keys.getString(i);
			
			if (key == uuid)
			{
				JSONObject current_player_json = new JSONObject(players_json.get(uuid));
				ArrayList<String> muted_players = new ArrayList<String>();
				for (int j = 0; j < current_player_json.length(); j ++)
				{
					muted_players.add(current_player_json.names().getString(j));
				}
				return muted_players;
			}
		}
		return null;
	}
	
    @SuppressWarnings({ "static-access", "resource" })
	@SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) throws IOException 
    {
    	
    	String message = event.getMessage().getUnformattedComponentText();
    	ArrayList<String> muted_players = getMutedPlayers(Minecraft.getInstance().player.getUUID(Minecraft.getInstance().player.getGameProfile()).toString());
    	MinecraftServer minecraft_server = ServerLifecycleHooks.getCurrentServer();
    	
    	if (minecraft_server != null)
    	{
    		for (ServerPlayerEntity player : minecraft_server.getPlayerList().getPlayers())
    		{
    			for (String muted_player : muted_players)
    			{
    				if (player.getUUID(player.getGameProfile()).toString() == muted_player && message.contains(player.getDisplayName().toString()))
    				{
    					event.setCanceled(true);
    				}
    			}
    		}
    	}
    }
    
    
    
    
}
