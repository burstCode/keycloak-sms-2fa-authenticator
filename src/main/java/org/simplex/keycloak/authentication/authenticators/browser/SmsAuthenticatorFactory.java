package org.simplex.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class SmsAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "sms-otp";

    private static final SmsAuthenticator SINGLETON = new SmsAuthenticator();

    @Override
    public String getDisplayType() {
        return "SMS OTP";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Two-factor authentication by sending" +
                " confirmation SMS code to the user's mobile phone number";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty codeLengthProperty = new ProviderConfigProperty(
                "codeLength",
                "Confirmation code length",
                "Length of the generated acknowledgement code (4 to 9 digits)",
                ProviderConfigProperty.LIST_TYPE,
                "6"
        );
        List<String> options = List.of("4", "5", "6", "7", "8", "9");
        codeLengthProperty.setOptions(options);

        return List.of(
                // Изменяемые параметры, доступные в админ-панели Keycloak
                new ProviderConfigProperty(
                        "smscLogin",
                        "SMSC login",
                        "SMSC login to send sms",
                        ProviderConfigProperty.STRING_TYPE,
                        ""),
                new ProviderConfigProperty(
                        "smscPassword",
                        "SMSC password",
                        "SMSC password to send sms",
                        ProviderConfigProperty.STRING_TYPE,
                        ""
                ),
                new ProviderConfigProperty(
                        "codeTimeToLive",
                        "TTL кода",
                        "Code lifetime (validity) in seconds",
                        ProviderConfigProperty.STRING_TYPE,
                        "300"
                ),
                codeLengthProperty,
                new ProviderConfigProperty(
                        "phoneNumberAttribute",
                        "Название аттрибута для номера телефона",
                        "It is important that it matches <User>->Attributes->Key," +
                                " where the phone number is stored",
                        ProviderConfigProperty.STRING_TYPE,
                        "phoneNumber"
                )
        );
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
