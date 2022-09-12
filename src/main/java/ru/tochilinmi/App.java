package ru.tochilinmi;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class App{
	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		properties.load(new FileInputStream("app.properties"));
//		String TOKEN = new TelegramBot();

		//пока вместо базы
		Map<Long, User> users = new HashMap<>();

		TelegramBot bot = new TelegramBot(properties.getProperty("telegram_token"));

		bot.setUpdatesListener(updates -> {
			updates.forEach(System.out::println);

			updates.forEach(update -> {
//				Integer userId = update.message().from().id();
				// В лонг так как некоторые id не помещаются в int, на php проверил
				Long userId = update.message().from().id();
				// проверка наличия логина и пароля
				if (!users.containsKey(userId)){
					bot.execute(new SendMessage(
							update.message().chat().id(),
							"Вам необходимо прислать логин и пароль в одном предложении через пробел"
					));
					users.put(userId, null);
				} else if(users.get(userId) == null){ // Запись логина и пароля
					String[] loginAndPassword = update.message().text().split(" ");
					User user = new User(loginAndPassword[0], loginAndPassword[1]);
					users.put(userId, user);
					bot.execute(new SendMessage(
							update.message().chat().id(),
							"Всё работает! \n Теперь Вы можете присылать нам текст/изображение/геопозицию для"
									+ "Инстаграм (в одном сообщении)"
					));
				} else {
					System.out.println(update.toString());
					Post post = new Post();
					post.setTitle(update.message().text());
					GetFileResponse fileResponse = bot.execute( new GetFile(update.message().photo()[0].fileId()));
					String fullPath = bot.getFullFilePath(fileResponse.file());
					System.out.println(fullPath);
					try{
						HttpDownload.downloadFile(fullPath,"./images",update.message().photo()[0].fileId()+".jpg");
					} catch(IOException e){
						System.err.println(e.getMessage());
					}
					post.setPhoto(new File("./images/"+update.message().photo()[0].fileId()+".jpg").getPath());
					users.get(userId).addPost(post);

					System.out.println(users.toString());
				}
			});

			return UpdatesListener.CONFIRMED_UPDATES_ALL;
		});
	}
}
