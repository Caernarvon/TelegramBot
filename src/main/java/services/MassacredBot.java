package services;

import org.jetbrains.annotations.NotNull;

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
import resources.PhotoTriggerEntity;
import resources.UserEntity;

import java.text.MessageFormat;
import java.util.*;

import static constants.Properties.BOT_NAME;
import static constants.Properties.BOT_TOKEN;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class MassacredBot extends TelegramLongPollingBot {

    /**
     * List of userEntities to keep warnings and userEntities info.
     */
    private List<UserEntity> userEntities = new ArrayList<>();

    /**
     * List of userEntities to keep swear words.
     */
    private List<String> swearWords = new ArrayList<>();

    /**
     * List of photo to show when user triggers it.
     */
    private List<PhotoTriggerEntity> photoTriggerEntities = new ArrayList<>();

    public MassacredBot(DefaultBotOptions botOptions) {
        super(botOptions);
    }


    /**
     * Method receives updates to process them later.
     */
    public void onUpdateReceived(Update update) {
        if (!findUser(update).isPresent()) {
            addUser(update);
        }

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
         * resources.UserEntity joins group and receives welcome message and list of rules.
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
     * Method processes commands. Allows to divide userEntities and admins when processing
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
        PhotoTriggerEntity photoTriggerEntity = new PhotoTriggerEntity();
        update.getMessage().getPhoto().stream().forEach(photo -> {
                    photoTriggerEntity.setPhoto(photo);
                    photoTriggerEntity.setTriggerText(update.getMessage().getCaption().substring(10).trim());
                }
        );
        if (!photoTriggerEntities.contains(photoTriggerEntity)) {
            photoTriggerEntities.add(photoTriggerEntity);
            sendMessage("Photo added successfully", update.getMessage().getChatId());
        }
    }

    /**
     * Method adds swear word as trigger.
     *
     * @param update contains swear word.
     */
    private void addSwearWord(@NotNull Update update) {
        if (!swearWords.contains(update.getMessage().getText().substring(15).trim())) {
            swearWords.add(update.getMessage().getText().substring(15).trim());
            sendMessage("'" + update.getMessage().getText().substring(15).trim() + "' is added as swear word",
                    update.getMessage().getChatId());
        }
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
        for (PhotoTriggerEntity photoTriggerEntity : photoTriggerEntities) {
            if (update.getMessage().getText().contains(photoTriggerEntity.getTriggerText())) {
                sendPhoto(photoTriggerEntity.getPhoto(), update.getMessage().getChatId());
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
        swearWords.stream().forEach(swearWord -> {
            if (update.getMessage().getText().contains(swearWord)) {
                findUser(update).ifPresent(userEntity -> {
                    warnSwearingUser(update, swearWord);
                });
            }
        });
    }

    /**
     * Method sends warning in group chat and adds 1 warning to number of user's warnings.
     * resources.UserEntity will be banned for 1 day if he gets 4 warnings.
     *
     * @param update        contains message text.
     * @param forbiddenWord forbidden word to display in warning.
     */
    private void warnSwearingUser(Update update, String forbiddenWord) {
        findUser(update).ifPresent(userEntity -> {
            userEntity.setWarnings(userEntity.getWarnings() + 1);
            if (userEntity.getWarnings() == 4) {
                userEntities.remove(userEntity);
                sendMessage("User banned for 1 day", update.getMessage().getChatId());
                try {
                    banUser(update);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                String numOfWarnings;
                if (userEntity.getWarnings() == 1) {
                    numOfWarnings = "st";
                } else if (userEntity.getWarnings() == 2) {
                    numOfWarnings = "nd";
                } else if (userEntity.getWarnings() == 3) {
                    numOfWarnings = "rd";
                } else {
                    numOfWarnings = "th";
                }
                sendMessage(MessageFormat.format("Word ''{0}'' is forbidden, {1}{2} warning!", forbiddenWord,
                        userEntity.getWarnings(), numOfWarnings), update.getMessage().getChatId());
            }
        });
    }

    /**
     * Method bans user for 1 day.
     *
     * @param update contains chat id and user id.
     */
    private void banUser(@NotNull Update update) throws TelegramApiException {
        int date = (int) (new Date().getTime() / 1000) + (86400);
        KickChatMember kickChatMember = new KickChatMember()
                .setChatId(update.getMessage().getChatId())
                .setUserId(update.getMessage().getFrom().getId())
                .setUntilDate(date);

        sendApiMethod(kickChatMember);
    }

    /**
     * Method to send message in group.
     *
     * @param text   what text to send.
     * @param chatId where to send.
     */
    private void sendMessage(String text, long chatId) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(chatId)
                .setText(text);
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
        SendPhoto sendPhoto = new SendPhoto()
                .setChatId(chatId)
                .setPhoto(photoSize.getFileId());
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to add userEntities to userEntities list. Used to help to keep user's warnings.
     *
     * @param update contains first name, last name, user name and user id.
     */
    private void addUser(Update update) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(update.getMessage().getFrom().getId());
        userEntity.setFirstName(update.getMessage().getFrom().getFirstName());
        userEntity.setLastName(update.getMessage().getFrom().getLastName());
        userEntity.setUsername(update.getMessage().getFrom().getUserName());
        if (!userEntities.contains(userEntity)) {
            userEntities.add(userEntity);
        }

    }

    /**
     * Method to find user.
     *
     * @param update contains user id.
     */
    @NotNull
    private Optional<UserEntity> findUser(Update update) {
        return userEntities.stream()
                .filter(userEntity -> userEntity.getUserId().equals(update.getMessage().getFrom().getId()))
                .findFirst();
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
}
