import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MyTelegramBot extends TelegramLongPollingBot {
    private Map<String, String> loginSteps = new HashMap<>(); // Изменение типа ключа на String
    private static final String username = "Behaplayer";
    private static final String password = "beh123mut1195";
    private static final String BOT_TOKEN = "7711312557:AAFMddYUfOPSS9diRh4XFtshysjZkuyJ1Mc";
    private static final String BOT_USERNAME = "behaplayerBot";

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String userMessage = message.getText();
            Long chatId = message.getChatId();

            if (userMessage.equals("/start")) {
                sendTextMessage(chatId, "Привет! Я ваш новый бот.");
            } else if (userMessage.equals("/help")) {
                sendTextMessage(chatId, "Я могу помочь вам с базовыми функциями!");
            } else if (userMessage.equals("/login")) {
                sendTextMessage(chatId, "Для того чтобы войти в аккаунт, введите username: ");
                loginSteps.put(chatId.toString(), "WAITING_USERNAME");
            } else if (loginSteps.containsKey(chatId.toString())) {
                if (loginSteps.get(chatId.toString()).equals("WAITING_USERNAME")) {
                    String inUsername = userMessage;
                    sendTextMessage(chatId, "А также пароль: ");
                    loginSteps.put(chatId.toString(), "WAITING_PASSWORD");
                    loginSteps.put(chatId + "_username", inUsername);
                } else if (loginSteps.get(chatId.toString()).equals("WAITING_PASSWORD")) {
                    String inUsername = loginSteps.get(chatId + "_username");
                    String inPassword = userMessage;
                    loginSteps.remove(chatId.toString());
                    loginSteps.remove(chatId + "_username");

                    if (Objects.equals(inUsername, username) && Objects.equals(inPassword, password)) {
                        sendTextMessage(chatId, "Добро пожаловать " + username + ", Вы успешно вошли в аккаунт!");
                    } else {
                        sendTextMessage(chatId, "Неверное имя пользователя или пароль. Попробуйте еще раз.");
                    }
                }
            } else {
                sendTextMessage(chatId, "Вы написали: " + userMessage);
            }
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyTelegramBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
