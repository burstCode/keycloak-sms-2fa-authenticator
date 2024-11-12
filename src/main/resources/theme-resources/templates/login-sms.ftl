<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section == "header">
        <!-- Заголовок страницы -->
        Вход с помощью SMS-кода
    <#elseif section == "show-username">
        <h1>На ваш номер телефона отправлен код подтверждения</h1>
    <#elseif section == "form">
        <!-- Форма ввода кода -->
        <form id="kc-sms-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="smsCode" class="${properties.kcLabelClass!}">Введите код подтверждения:</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="smsCode" name="smsCode" class="${properties.kcInputClass!}" required autofocus/>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="Подтвердить"/>
                </div>
            </div>
        </form>
    <#elseif section == "info">
        <!-- Информация для пользователя -->
    </#if>
</@layout.registrationLayout>
