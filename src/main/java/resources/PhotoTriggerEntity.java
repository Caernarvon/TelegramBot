package resources;

import org.telegram.telegrambots.meta.api.objects.PhotoSize;

/**
 * Class associates photo with text.
 */
public class PhotoTriggerEntity {
    private PhotoSize photo;
    private String triggerText;

    public PhotoSize getPhoto() {
        return photo;
    }

    public void setPhoto(PhotoSize photo) {
        this.photo = photo;
    }

    public String getTriggerText() {
        return triggerText;
    }

    public void setTriggerText(String triggerText) {
        this.triggerText = triggerText;
    }
}
