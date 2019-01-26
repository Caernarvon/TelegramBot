import org.telegram.telegrambots.meta.api.objects.PhotoSize;

/**
 * Class associates photo with text.
 */
class PhotoTrigger {
    private PhotoSize photo;
    private String triggerText;

    PhotoSize getPhoto() {
        return photo;
    }

    void setPhoto(PhotoSize photo) {
        this.photo = photo;
    }

    String getTriggerText() {
        return triggerText;
    }

    void setTriggerText(String triggerText) {
        this.triggerText = triggerText;
    }
}
