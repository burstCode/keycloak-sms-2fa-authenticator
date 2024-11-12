package org.simplex.keycloak.authentication.authenticators.browser;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmsAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(SmsAuthenticator.class.getName());
    private static final String TPL_CODE = "login-sms.ftl";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();

        final String PHONE_NUMBER_ATTRIBUTE = config.getConfig().get("phoneNumberAttribute");

        // Получение номера телефона из контекста, в случае отсутствия - создание страницы с ошибкой
        String phoneNumber = user.getFirstAttribute(PHONE_NUMBER_ATTRIBUTE);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            logger.log(Level.WARNING,"User " + user.getUsername() + "'s phone number was not found.");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("User phone number not found")
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));

            return;
        }

        // Генерация кода и отправка СМС
        int codeLength = Integer.parseInt(config.getConfig().get("codeLength"));
        String code = generateCode(codeLength);
        sendSms(phoneNumber, code, config);

        // Сохранение отправленного кода в контекст
        context.getAuthenticationSession().setAuthNote("smsCode", code);

        // Установка времени жизни кода
        int CODE_TTL_SECONDS = Integer.parseInt(config.getConfig().get("codeTimeToLive"));
        long expirationTime = System.currentTimeMillis() + (CODE_TTL_SECONDS * 1000L);
        context.getAuthenticationSession().setAuthNote("smsCodeTTL", Long.toString(expirationTime));

        // Отображение формы ввода СМС-кода
        context.challenge(context.form().createForm(TPL_CODE));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("smsCode");

        // Проверка: ввел ли пользователь код
        if (enteredCode == null || enteredCode.isEmpty()) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setError("Please enter the confirmation code")
                            .createForm(TPL_CODE));
            return;
        }

        // Получение сгенерированного кода и времени его жизни
        String expectedCode = authSession.getAuthNote("smsCode");
        String expirationTimeString = authSession.getAuthNote("smsCodeTTL");

        if (expectedCode == null || expirationTimeString == null) {
            logger.log(Level.SEVERE, "Failed to get the generated code or its lifetime");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        // Проверка истечения времени действия кода
        long expirationTime = Long.parseLong(expirationTimeString);
        if (System.currentTimeMillis() > expirationTime) {
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                    context.form().setError("Code expired")
                            .createErrorPage(Response.Status.BAD_REQUEST));
            return;
        }

        // Проверка соответствия введенного кода
        if (!enteredCode.equals(expectedCode)) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form().setError("Invalid confirmation code")
                            .createForm(TPL_CODE));
            return;
        }

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Генерация случайного N-значного числа
    private String generateCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    // Отправка смс с кодом подтверждения через СМСЦентр
    private void sendSms(String phoneNumber, String code, AuthenticatorConfigModel config) {
        String login = config.getConfig().get("smscLogin");
        String password = config.getConfig().get("smscPassword");
        String message = "Ваш код подтверждения: " + code;
        
        boolean configMissed = false;

        // Проверка данных СМСЦентра в конфиге
        if (login == null || login.isEmpty()) {
            logger.severe("SMSC login is missing in the configuration.");
            configMissed = true;
        }
        if (password == null || password.isEmpty()) {
            logger.severe("SMSC password is missing in the configuration");
            configMissed = true;
        }

        if (configMissed) {
            return;
        }

        String urlString = "";

        // Составляем запрос к СМСЦентру и кодируем все строки в UTF-8
        try {
            urlString = String.format(
                    "https://smsc.ru/sys/send.php?login=%s&psw=%s&phones=%s&mes=%s&charset=utf-8",
                    URLEncoder.encode(login, "UTF-8"),
                    URLEncoder.encode(password, "UTF-8"),
                    URLEncoder.encode(phoneNumber, "UTF-8"),
                    URLEncoder.encode(message, "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE,
                    "Error when forming SMSC link: " + e);
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            logger.log(Level.INFO, "Attempting to access the SMSC via the link: " + urlString);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.log(Level.WARNING,
                        "Error sending SMS to a number " + phoneNumber + ", response code: " + responseCode);
            } else {
                String lastTwoDigitsOfCode = code.substring(code.length() - 2);

                StringBuilder dashes = new StringBuilder();
                for (int i = 0; i < code.length() - 2; i++) {
                    dashes.append("-");
                }

                String formattedCodeOutput = dashes + lastTwoDigitsOfCode; // Форматируем вывод

                logger.log(Level.INFO,
                        "SMS successfully sent to the number: " + phoneNumber +
                                ", last 2 digits of the confirmation code: " + formattedCodeOutput);


            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error when sending SMS to a number: " + phoneNumber, e);
        }
    }
}
