package com.ncba.countryinfo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * HTTP transport for the SOAP service. Connect and read timeouts make sure a slow
 * upstream can never hold a request thread indefinitely.
 */
@Configuration
@EnableConfigurationProperties(SoapClientProperties.class)
public class SoapClientConfig {

    @Bean
    public RestClient soapRestClient(SoapClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)   // plain SOAP 1.1 over HTTP/1.1
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.endpointUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
