import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.MassacredBot;

import static constants.Properties.PROXY_HOST;
import static constants.Properties.PROXY_PORT;

public class Main {

    public static void main(String[] args) {
        ApiContextInitializer.init();
        try {
            /*
             * Creating the TelegramBotsApi object to register bot.
             */
            TelegramBotsApi botsApi = new TelegramBotsApi();

            /*
             * Setting up Http proxy.
             */
            DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
            HttpHost httpHost = new HttpHost(PROXY_HOST, PROXY_PORT);

            RequestConfig requestConfig = RequestConfig.custom().setProxy(httpHost).setAuthenticationEnabled(false).build();
            botOptions.setRequestConfig(requestConfig);
            botOptions.setProxyHost(PROXY_HOST);
            botOptions.setProxyPort(PROXY_PORT);

            /*
             * Registering newly created LongPollingBot.
             */
            MassacredBot bot = new MassacredBot(botOptions);

            botsApi.registerBot(bot);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
