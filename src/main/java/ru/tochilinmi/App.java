package ru.tochilinmi;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import ru.tochilinmi.entities.PostEntity;
import ru.tochilinmi.entities.UserEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class App{
	public static void main(String[] args) throws IOException {

		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("instabot");

		Properties properties = new Properties();
		properties.load(new FileInputStream("app.properties"));
		TelegramBot bot = new TelegramBot(properties.getProperty("telegram_token"));

		bot.setUpdatesListener(updates -> {
//			updates.forEach(update->{
//				System.out.println(update);
//				System.out.println("\n end message\n\n");
//			});
//			updates.forEach(System.out::println);
			updates.forEach(update -> {
				Long userId = update.message().from().id();
				EntityManager manager = entityManagerFactory.createEntityManager();
				manager.getTransaction().begin();
				UserEntity user = manager.find(UserEntity.class,userId);
				if(user==null){//Проверка наличия пользователя в MongoDb
					bot.execute(new SendMessage(
							update.message().chat().id(),
							"Вам необходимо прислать логин и пароль в одном предложении через пробел"
					));
					manager.persist(new UserEntity(update.message().from().id(), null, null));
//					manager.getTransaction().commit();
				} else if(user.getLogin() == null){ // Запись логина и пароля
					String[] loginAndPassword = update.message().text().split(" ");
					user.setLogin(loginAndPassword[0]);
					user.setPassword(loginAndPassword[1]);

					manager.persist(user);
					bot.execute(new SendMessage(userId,
							"Всё работает! \nТеперь Вы можете присылать нам текст/изображение для"
									+ " Инстаграм (в одном сообщении)"
					));
//					manager.getTransaction().commit();
				} else if(user.getLogin() != null && update.message().photo().length > 0){

					GetFileResponse fileResponse = bot.execute( new GetFile(update.message().photo()[0].fileId()));
					String fullPath = bot.getFullFilePath(fileResponse.file());
					try{
						HttpDownload.downloadFile(fullPath,"./images",update.message().photo()[0].fileId()+".jpg");
					} catch(IOException e){
						System.err.println(e.getMessage());
					}
					PostEntity post = new PostEntity();
					post.setTitle(update.message().caption());
					post.setPhoto(new File("./images/" + update.message().photo()[0].fileId()+".jpg").getPath());
					user.addPost(post);

					manager.persist(user);
					manager.persist(post);
				}
				manager.getTransaction().commit();
				manager.close();
			});

			return UpdatesListener.CONFIRMED_UPDATES_ALL;
		});
	}
}
