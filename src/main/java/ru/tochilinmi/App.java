package ru.tochilinmi;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import org.brunocvcunha.instagram4j.Instagram4j;
import org.brunocvcunha.instagram4j.requests.InstagramUploadPhotoRequest;
import ru.tochilinmi.database.DatabaseConnector;
import ru.tochilinmi.entities.PostEntity;
import ru.tochilinmi.entities.UserEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App{
	public static void main(String[] args) throws IOException {

		DatabaseConnector connector = new DatabaseConnector("instabot");
		//cron
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

		//upload settings
		Properties properties = new Properties();
		properties.load(new FileInputStream("app.properties"));
		TelegramBot bot = new TelegramBot(properties.getProperty("telegram_token"));

		bot.setUpdatesListener(updates -> {

			updates.forEach(update -> {
				Long userId = update.message().from().id();
				connector.startTransaction();
				UserEntity user = connector.getUserService().findById(userId);
				if(user==null){//Check for user exist in MongoDb
					bot.execute(new SendMessage(
							update.message().chat().id(),
							"Вам необходимо прислать логин и пароль в одном предложении через пробел"
					));
					connector.getUserService().save( new UserEntity(userId, null,null));
				} else if(user.getLogin() == null){ // Set the login and the password
					String[] loginAndPassword = update.message().text().split(" ");
					user.setLogin(loginAndPassword[0]);
					user.setPassword(loginAndPassword[1]);

					connector.getUserService().save(user);
					bot.execute(new SendMessage(userId,
							"Всё работает! \nТеперь Вы можете присылать нам текст, изображение и дату публикациии"+
									"(формат: чч:мм дд.мм.гггг, в начале сообщения) для" +
									" Инстаграм (в одном сообщении)"
					));
				} else if(user.getLogin() != null && update.message().photo().length > 0){

					GetFileResponse fileResponse = bot.execute( new GetFile(update.message().photo()[0].fileId()));
					String fullPath = bot.getFullFilePath(fileResponse.file());
					try{
						HttpDownload.downloadFile(fullPath,"./images",update.message().photo()[0].fileId()+".jpg");
					} catch(IOException e){
						System.err.println(e.getMessage());
					}
					SimpleDateFormat dateParser = new SimpleDateFormat("hh:mm dd.MM.yyyy");
					Date datePost = null;
					try {
						datePost = dateParser.parse(update.message().caption().split("\n")[0]);
					} catch (ParseException e) {
						System.err.println(e.getMessage());
					}

					Runnable sendToInstagram = () -> {
						Instagram4j instagram4j = Instagram4j.builder()
								.username(user.getLogin())
								.password(user.getPassword())
								.build();
						instagram4j.setup();
						try {
							instagram4j.login();
							instagram4j.sendRequest(new InstagramUploadPhotoRequest(
									new File("./images/" + update.message().photo()[0].fileId() + ".jpg"),
									update.message().caption().replace(
											update.message().caption().split("\n")[0] + "\n",
											"")));
						} catch (IOException e){
							System.err.println("Instagram error: " + e.getMessage());
						}

					};

					System.out.println(datePost.getTime() - System.currentTimeMillis());
					scheduler.schedule(
							sendToInstagram,
							datePost.getTime() - System.currentTimeMillis(),
							TimeUnit.MILLISECONDS
							);

					PostEntity post = new PostEntity();
					post.setDate(datePost);
					post.setTitle(update.message().caption().replace(
							update.message().caption().split("\n")[0]+"\n",
							""));
					post.setPhoto(new File("./images/" + update.message().photo()[0].fileId()+".jpg").getPath());
					user.addPost(post);

					bot.execute(new SendMessage(userId,
							"Отлично!\nПост будет опубликован вовремя."
					));

					connector.getUserService().save(user);
					connector.getPostService().save(post);
				}
				connector.endTransaction();
			});

			return UpdatesListener.CONFIRMED_UPDATES_ALL;
		});
	}
}
