import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import me.mrletsplay.mrcore.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class AOCStonks extends ListenerAdapter {

	private static class DayStonks {
		int day, both, firstonly, total;
	}

	public static void main(String[] args) throws Exception {
		JSONObject config = new JSONObject(Files.readString(Path.of("config.json")));

		JDA jda = JDABuilder.createDefault(config.getString("token"))
				.build()
				.awaitReady();
		jda.addEventListener(new AOCStonks());

		Guild guild = jda.getGuildById(config.getString("guildId"));
		MessageChannel ch = guild.getChannelById(MessageChannel.class, config.getString("channelId"));

		jda.updateCommands()
			.addCommands(Commands.slash("stonks", "Show me some stonks"))
			.complete();

		while(true) {
			try {
				ch.sendMessage("```\n" + buildStonks() + "```").queue();
			}catch(Exception e) {
				ch.sendMessage("Failed to retrieve stonks :(").queue();
			}
			Thread.sleep(1000 * 60 * 60 * 6);
		}
	}

	private static String buildStonks() throws IOException {
		Document doc = Jsoup.parse(new URL("https://adventofcode.com/2022/stats"), 1000);

		List<DayStonks> stonks = new ArrayList<>();
		Element stats = doc.getElementsByClass("stats").get(0);
		for(Element e : stats.getElementsByTag("a")) {
			DayStonks s = new DayStonks();
			s.day = Integer.parseInt(e.ownText());
			s.both = Integer.parseInt(e.child(0).text().trim());
			s.firstonly = Integer.parseInt(e.child(1).text().trim());
			s.total = s.both + s.firstonly;
			stonks.add(s);
		}

		Collections.reverse(stonks);

		String tot = "";
		for(int i = 0; i < stonks.size(); i++) {
			DayStonks s = stonks.get(i);
			DayStonks p = i == 0 ? null : stonks.get(i-1);
			int quit = p == null ? 0 : p.total - s.total;
			tot += String.format("%02d: First only: %8d | Both: %8d | Both Percent: %05.2f%% | Quit: %05.2f%%\n", s.day, s.firstonly, s.both, (s.both / (float) s.total) * 100, quit / (float) (p == null ? 1 : p.total) * 100);
		}

		return tot;
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			event.reply("```\n" + buildStonks() + "```").queue();
		} catch (IOException e) {
			event.reply("Oh no, something broke :(").queue();
		}
	}

}
