package br.com.opin.mopclient.gateway;


import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Bootstraps and configures the Gateway context.
 * Defines the application entry point and prepares global resources
 * like cache and startup metrics to keep performance predictable.
 */
@SpringBootApplication(proxyBeanMethods = false)
@EnableCaching
public final class GatewayApplication {

	private GatewayApplication() {
		// Impede instanciação acidental; a classe deve ser usada apenas estaticamente.
	}

	/**
	 * Método principal que prepara o {@link SpringApplication} com ajustes de
	 * performance antes de iniciar o contexto Spring.
	 *
	 * @param args argumentos passados pela linha de comando.
	 */
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(GatewayApplication.class);
		application.setLazyInitialization(true);
		application.setBannerMode(Banner.Mode.CONSOLE);
		application.setApplicationStartup(new BufferingApplicationStartup(2048));
		application.run(args);
	}
}
