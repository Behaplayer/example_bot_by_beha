import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyTelegramBot extends TelegramLongPollingBot {
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private static final String BOT_USERNAME = "behaplayerBot";
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

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
            String userMessage = message.getText().toLowerCase();
            Long chatId = message.getChatId();
            String senderUsername = message.getFrom().getUserName();
            String senderFN = message.getFrom().getFirstName();

            switch (userMessage.split(" ")[0]) {
                case "/start":
                    sendTextMessage(chatId, "Привет! Я бот от @behaplayer. Введите /help для списка команд.");
                    startMenuButtons(chatId);
                    break;

                case "/help":
                    showHelp(chatId);
                    break;

                case "/signin":
                    signIn(chatId, senderUsername, senderFN);
                    break;

                case "/signup":
                    signUp(chatId, senderUsername);
                    break;

                case "/balance":
                    showBalance(chatId, senderUsername);
                    break;

                case "/deposit":
                    handleDeposit(chatId, senderUsername, userMessage);
                    break;

                case "/withdraw":
                    handleWithdraw(chatId, senderUsername, userMessage);
                    break;

                default:
                    sendTextMessage(chatId, "Неверная команда. Введите /help для списка команд.");
            }
        }
    }

    private void showHelp(Long chatId) {
        StringBuilder helpText = new StringBuilder("Список команд:\n");
        for (String[] command : commandsList) {
            helpText.append(command[0]).append(" - ").append(command[1]).append(".\n");
        }
        sendTextMessage(chatId, helpText.toString());
    }

    private void signIn(Long chatId, String username, String firstName) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT is_authorized FROM users WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();
                if (rs.next() && rs.getBoolean("is_authorized")) {
                    sendTextMessage(chatId, "Добро пожаловать, " + firstName + "!");
                } else {
                    sendTextMessage(chatId, "Вы не авторизованы, введите /signup для авторизации.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void signUp(Long chatId, String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO users (username, is_authorized) VALUES (?, true) ON CONFLICT (username) DO NOTHING";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    String balanceQuery = "INSERT INTO balances (username, balance) VALUES (?, 0)";
                    try (PreparedStatement balanceStatement = connection.prepareStatement(balanceQuery)) {
                        balanceStatement.setString(1, username);
                        balanceStatement.executeUpdate();
                    }
                    sendTextMessage(chatId, "Вы успешно авторизованы! Теперь вы можете войти с помощью /signin.");
                } else {
                    sendTextMessage(chatId, "Вы уже авторизованы.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showBalance(Long chatId, String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT balance FROM balances WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    sendTextMessage(chatId, "Ваш баланс: " + rs.getInt("balance") + "$");
                } else {
                    sendTextMessage(chatId, "Вы не авторизованы. Введите /signup для авторизации.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDeposit(Long chatId, String username, String userMessage) {
        try {
            int amount = Integer.parseInt(userMessage.split(" ")[1]);
            if (amount <= 0) {
                sendTextMessage(chatId, "Сумма пополнения должна быть положительной.");
                return;
            }
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "UPDATE balances SET balance = balance + ? WHERE username = ?";
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setInt(1, amount);
                    statement.setString(2, username);
                    statement.executeUpdate();
                    showBalance(chatId, username);
                }
            }
        } catch (NumberFormatException e) {
            sendTextMessage(chatId, "Пожалуйста, укажите корректную сумму для пополнения. Пример: /deposit 100");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleWithdraw(Long chatId, String username, String userMessage) {
        try {
            int amount = Integer.parseInt(userMessage.split(" ")[1]);
            if (amount <= 0) {
                sendTextMessage(chatId, "Сумма снятия должна быть положительной.");
                return;
            }
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT balance FROM balances WHERE username = ?";
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, username);
                    ResultSet rs = statement.executeQuery();
                    if (rs.next() && rs.getInt("balance") >= amount) {
                        String updateQuery = "UPDATE balances SET balance = balance - ? WHERE username = ?";
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                            updateStatement.setInt(1, amount);
                            updateStatement.setString(2, username);
                            updateStatement.executeUpdate();
                            sendTextMessage(chatId, "Снято " + amount + "$.");
                            showBalance(chatId, username);
                        }
                    } else {
                        sendTextMessage(chatId, "Недостаточно средств.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            sendTextMessage(chatId, "Пожалуйста, укажите корректную сумму для снятия. Пример: /withdraw 100");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startMenuButtons(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите команду:");
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/help");
        row1.add("/balance");
        keyboard.add(row1);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
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
