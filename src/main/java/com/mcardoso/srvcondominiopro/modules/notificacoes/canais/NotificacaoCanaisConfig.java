package com.mcardoso.srvcondominiopro.modules.notificacoes.canais;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ResendProperties.class, TwilioProperties.class})
public class NotificacaoCanaisConfig {
}
