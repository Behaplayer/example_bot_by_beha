import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

public class MyTelegramBot extends TelegramLongPollingBot {
    private final Map<String, Boolean> usersDB = new HashMap<>();
    private final Map<String, Integer> userBalances = new HashMap<>(); // Хранение баланса пользователей
    private static final String BOT_TOKEN = "7711312557:AAFMddYUfOPSS9diRh4XFtshysjZkuyJ1Mc";
    private static final String BOT_USERNAME = "behaplayerBot";
    private static final String[][] commandsList = {
            {"/start", "Запуск бота"},
            {"/help", "Список команд"},
            {"/signin", "Войти в аккаунт"},
            {"/signup", "Авторизоваться"},
            {"/balance", "Показать баланс"},
            {"/deposit", "Пополнить баланс"},
            {"/withdraw", "Снять средства"}
    };

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
            String senderUsername = message.getFrom().getUserName();
            String senderFN = message.getFrom().getFirstName();
            String senderLN = message.getFrom().getLastName();

            switch (userMessage.toLowerCase()) {
                case "/start":
                    sendTextMessage(chatId, "Привет! Я бот от @behaplayer. Для показа списка команд введите /help.");
                    sendMenuButtons(chatId);
                    break;

                case "/help":
                case "помощь":
                    sendTextMessage(chatId, "Список команд:");
                    StringBuilder commandListText = new StringBuilder();
                    for (String[] command : commandsList) {
                        commandListText.append(command[0]).append(" - ").append(command[1]).append(".\n");
                    }
                    sendTextMessage(chatId, commandListText.toString());
                    break;

                case "/signin":
                case "войти":
                    if (usersDB.getOrDefault(senderUsername, false)) {
                        sendTextMessage(chatId, "Добро пожаловать, " + senderFN + " " + senderLN + "!");
                    } else {
                        sendTextMessage(chatId, "Вы не авторизованы, " + senderUsername + "!");
                    }
                    break;

                case "/signup":
                case "авторизоваться":
                    if (!usersDB.containsKey(senderUsername)) {
                        usersDB.put(senderUsername, true); // Сохраняем пользователя как авторизованного
                        userBalances.put(senderUsername, 0); // Устанавливаем начальный баланс 0
                        sendTextMessage(chatId, "Вы успешно авторизованы!");
                    } else {
                        sendTextMessage(chatId, "Вы уже авторизованы.");
                    }
                    break;

                case "/balance":
                case "Баланс":
                    showBalance(chatId, senderUsername);
                    break;

                case "/deposit":
                    deposit(chatId, senderUsername, 100); // Пополнение на 100 для примера
                    break;

                case "/withdraw":
                    withdraw(chatId, senderUsername, 50); // Снятие 50 для примера
                    break;

                default:
                    sendTextMessage(chatId, "Посмотрите список команд через /help или нижнее меню.");
            }
        }
    }

    private void showBalance(Long chatId, String username) {
        int balance = userBalances.getOrDefault(username, 0);
        sendTextMessage(chatId, "Ваш баланс: " + balance + "$");
    }

    private void deposit(Long chatId, String username, int amount) {
        if (usersDB.getOrDefault(username, false)) { // Проверка авторизации
            userBalances.put(username, userBalances.getOrDefault(username, 0) + amount);
            sendTextMessage(chatId, "Вы пополнили баланс на " + amount + "$. Ваш новый баланс: " + userBalances.get(username) + "$");
        } else {
            sendTextMessage(chatId, "Пожалуйста, авторизуйтесь для выполнения этой операции.");
        }
    }

    private void withdraw(Long chatId, String username, int amount) {
        if (usersDB.getOrDefault(username, false)) { // Проверка авторизации
            int currentBalance = userBalances.getOrDefault(username, 0);
            if (currentBalance >= amount) {
                userBalances.put(username, currentBalance - amount);
                sendTextMessage(chatId, "Вы сняли " + amount + "$. Ваш новый баланс: " + userBalances.get(username) + "$");
            } else {
                sendTextMessage(chatId, "Недостаточно средств для снятия.");
            }
        } else {
            sendTextMessage(chatId, "Пожалуйста, авторизуйтесь для выполнения этой операции.");
        }
    }

    private void sendMenuButtons(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Войти");
        row1.add("Авторизоваться");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Помощь");
        row2.add("Баланс");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
