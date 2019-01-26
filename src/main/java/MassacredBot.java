import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class MassacredBot extends TelegramLongPollingBot {

    private static final String BOT_NAME = "";
    private static final String TOKEN = "";

    /**
     * List of users to keep warnings and users info.
     */
    private List<User> users = new ArrayList<>();

    /**
     * List of users to keep swear words.
     */
    private List<String> swearWords = new ArrayList<>();

    /**
     * List of photo to show when user triggers it.
     */
    private List<PhotoTrigger> photoTriggers = new ArrayList<>();

    MassacredBot(DefaultBotOptions botOptions) {
        super(botOptions);
    }


    /**
     * Method receives updates to process them later.
     */
    public void onUpdateReceived(Update update) {

        /*
         * New message received.
         * Checking if message contains swear words.
         * Checking if message contain word that trigger photo show.
         */
        if ((update.hasMessage() && update.getMessage().hasText())) {
            checkAtSwearing(update);
            checkAtPhotoTrigger(update);

            /*
             * Message will be recognized as command if it starts with "/".
             */
            if (update.getMessage().getText().startsWith("/")) {
                processCommand(update);
            }

            /*
             * Checking if message contain photo with caption.
             */
        } else if (update.getMessage().hasPhoto() && !update.getMessage().getCaption().isEmpty()) {
            if (update.getMessage().getCaption().startsWith("/")) {
                processCommand(update);
            }
        }
        /*
         * User joins group and receives welcome message and list of rules.
         */
        else if (!update.getMessage().getNewChatMembers().isEmpty()) {
            printWelcomeMessage(update);
            addUser(update);
        }
    }


    /**
     * Method checks if user is group admin, returns 'true' if so.
     *
     * @param update contains info about chat id and message sender id (userId).
     */
    private boolean isGroupAdmin(@NotNull Update update) {
        GetChatAdministrators admins = new GetChatAdministrators().setChatId(getChatId(update));
        try {
            for (ChatMember member : sendApiMethod(admins)) {
                if (member.getUser().getId().equals(update.getMessage().getFrom().getId())) {
                    return true;
                }
            }
            return false;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Method processes commands. Allows to divide users and admins when processing
     * commands. Users can't add new triggers.
     *
     * @param update contains message text or caption text.
     */
    private void processCommand(@NotNull Update update) {
        if (isGroupAdmin(update)) {
            if (update.getMessage().getCaption() == null && update.getMessage().getText().startsWith("/add")) {
                addAsTrigger(update);
            } else if (update.getMessage().getText() == null && update.getMessage().getCaption().startsWith("/add photo")) {
                addPhoto(update);
            } else {
                sendMessage("Command isn't recognised", update.getMessage().getChatId());
            }

        } else if (!isGroupAdmin(update)) {
            if (update.getMessage().getText().startsWith("/add") || update.getMessage().getCaption().startsWith("/add")) {
                sendMessage("Only admins can add triggers", update.getMessage().getChatId());
            }
        } else {
            sendMessage("Command isn't recognised", update.getMessage().getChatId());
        }
    }

    /**
     * Method to add new triggers, presently it allows to add swear word or photo as
     * trigger. After addition it will be triggered on certain text in message.
     *
     * @param update contains message text, photo and caption.
     */
    private void addAsTrigger(@NotNull Update update) {
        if (update.getMessage().getText().startsWith("/add swear word")) {
            addSwearWord(update);
        } else if (update.getMessage().getCaption().startsWith("/add photo") && update.getMessage().hasPhoto()) {
            addPhoto(update);
        } else {
            sendMessage("Command isn't recognised", update.getMessage().getChatId());
        }
    }

    /**
     * Method adds photo as trigger.
     *
     * @param update contains photo and caption.
     */
    private void addPhoto(@NotNull Update update) {

        PhotoTrigger photoTrigger = new PhotoTrigger();
        for (PhotoSize photo : update.getMessage().getPhoto()) {
            photoTrigger.setPhoto(photo);
            photoTrigger.setTriggerText(update.getMessage().getCaption().substring(10).trim());
        }
        photoTriggers.add(photoTrigger);
        sendMessage("Photo added successfully", update.getMessage().getChatId());
    }

    /**
     * Method adds swear word as trigger.
     *
     * @param update contains swear word.
     */
    private void addSwearWord(@NotNull Update update) {
        swearWords.add(update.getMessage().getText().substring(15).trim());
        sendMessage("'" + update.getMessage().getText().substring(15).trim() + "' is added as swear word",
                update.getMessage().getChatId());
    }

    /**
     * Method prints welcome message and rules when user joins group.
     *
     * @param update contains user's first name and user's last name to welcome him.
     */
    private void printWelcomeMessage(@NotNull Update update) {
        sendMessage("Hello " +
                        update.getMessage().getNewChatMembers().get(0).getFirstName() +
                        " " +
                        update.getMessage().getNewChatMembers().get(0).getLastName() +
                        "!",
                update.getMessage().getChatId());
        sendMessage("LIST OF RULES : \n" +
                "DO NOT SWEAR!\n" +
                "DO NOT SPAM!", update.getMessage().getChatId());
    }

    /**
     * Method checks if message contains text that triggers photo.
     *
     * @param update contains message text.
     */
    private void checkAtPhotoTrigger(Update update) {
        for (PhotoTrigger photoTrigger : photoTriggers) {
            if (update.getMessage().getText().contains(photoTrigger.getTriggerText())) {
                sendPhoto(photoTrigger.getPhoto(), update.getMessage().getChatId());
            }
        }
    }

    /**
     * Method checks if message contains text with swear words. If true, method
     * calls {@code warnSwearingUser}.
     *
     * @param update contains message text.
     */
    private void checkAtSwearing(Update update) {
        for (String forbiddenWord : swearWords) {
            if (update.getMessage().getText().contains(forbiddenWord)) {
                if (findUser(update) != null) {
                    warnSwearingUser(update, forbiddenWord);
                } else {
                    addUser(update);
                    warnSwearingUser(update, forbiddenWord);
                }
            }
        }
    }

    /**
     * Method sends warning in group chat and adds 1 warning to number of user's warnings.
     * User will be banned for 1 day if he gets 4 warnings.
     *
     * @param update        contains message text.
     * @param forbiddenWord forbidden word to display in warning.
     */
    private void warnSwearingUser(Update update, String forbiddenWord) {
        Objects.requireNonNull(findUser(update)).setWarnings(Objects.requireNonNull(findUser(update)).getWarnings() + 1);
        if (Objects.requireNonNull(findUser(update)).getWarnings() == 4) {
            users.remove(findUser(update));
            sendMessage("User banned for 1 day", update.getMessage().getChatId());
            try {
                banUser(update);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            String numOfWarnings;
            if (Objects.requireNonNull(findUser(update)).getWarnings() == 1) {
                numOfWarnings = "st";
            } else if (Objects.requireNonNull(findUser(update)).getWarnings() == 2) {
                numOfWarnings = "nd";
            } else if (Objects.requireNonNull(findUser(update)).getWarnings() == 3) {
                numOfWarnings = "rd";
            } else {
                numOfWarnings = "th";
            }
            sendMessage(MessageFormat.format("Word ''{0}'' is forbidden, {1}{2} warning!", forbiddenWord,
                    Objects.requireNonNull(findUser(update)).getWarnings(), numOfWarnings), update.getMessage().getChatId());
        }
    }

    /**
     * Method bans user for 1 day.
     *
     * @param update contains chat id and user id.
     */
    private void banUser(@NotNull Update update) throws TelegramApiException {
        int date = (int) (new Date().getTime() / 1000) + (86400);
        KickChatMember kickChatMember = new KickChatMember();
        kickChatMember.setChatId(update.getMessage().getChatId());
        kickChatMember.setUserId(update.getMessage().getFrom().getId());
        kickChatMember.setUntilDate(date);

        sendApiMethod(kickChatMember);
    }

    /**
     * Method to send message in group.
     *
     * @param text   what text to send.
     * @param chatId where to send.
     */
    private void sendMessage(String text, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to send photo in group.
     *
     * @param photoSize what photo to send.
     * @param chatId    where to send.
     */
    private void sendPhoto(@NotNull PhotoSize photoSize, long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(photoSize.getFileId());
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to add users to users list. Used to help to keep user's warnings.
     *
     * @param update contains first name, last name, user name and user id.
     */
    private void addUser(@org.jetbrains.annotations.NotNull Update update) {
        User user = new User();
        user.setUserId(update.getMessage().getFrom().getId());
        user.setFirstName(update.getMessage().getFrom().getFirstName());
        user.setLastName(update.getMessage().getFrom().getLastName());
        user.setUsername(update.getMessage().getFrom().getUserName());
        users.add(user);

    }

    /**
     * Method to find user.
     *
     * @param update contains user id.
     */
    @Nullable
    private User findUser(Update update) {
        if (!users.isEmpty()) {
            for (User user : users) {
                if (user.getUserId().equals(update.getMessage().getFrom().getId())) {
                    return user;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return TOKEN;
    }
}
