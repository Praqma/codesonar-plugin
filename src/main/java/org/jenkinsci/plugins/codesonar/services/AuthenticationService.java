package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.List;
import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

public class AuthenticationService {
    private HttpService httpService;

    public AuthenticationService(HttpService httpService) {
        this.httpService = httpService;
    }

    // basic authentication
    public void authenticate(URI baseHubUri){

    }



    public void authenticate(URI baseHubUri, KeyStore keyStore, @Nonnull String password) throws AbortException {
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, password.toCharArray()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException ex) {
            throw new AbortException(String.format(
                "[CodeSonar] failed to authenticate.%n[CodeSonar] Message is: %s", ex.getMessage())
            );
        }

        HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext).build();

        httpService.setExecutor(Executor.newInstance(httpClient).use(httpService.getHttpCookieStore()));

        List<NameValuePair> loginForm = Form.form()
                .add("sif_use_tls", "yes")
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .build();

        int status = -1;
        
        try {
            HttpResponse resp = httpService.execute(Request.Post(baseHubUri.resolve("/sign_in.html"))
                    .bodyForm(loginForm))
                    .returnResponse();

            status = resp.getStatusLine().getStatusCode();

        } catch (IOException e) {
            throw new AbortException(String.format("[CodeSonar] %s", e.getMessage()));
        }
        
        if (status != HttpStatus.SC_OK) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] HTTP status code: %s", status));
        }
    }

    public void authenticate(URI baseHubUri, String username, String password) throws AbortException {
        List<NameValuePair> loginForm = Form.form()
                .add("sif_username", username)
                .add("sif_password", password)
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .build();

        int status = -1;
        
        try {
            HttpResponse resp = httpService.execute(Request.Post(baseHubUri.resolve("/sign_in.html"))
                    .bodyForm(loginForm))
                    .returnResponse();
            
            status = resp.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new AbortException(String.format("[CodeSonar] %s", e.getMessage()));
        }
        
        if (status != HttpStatus.SC_OK) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] HTTP status code: %s", status));
        }
    }

    public void signOut(URI baseHubUri) throws AbortException {
        try {
            HttpResponse resp = httpService.execute(Request.Get(baseHubUri.resolve("/sign_out.html"))).returnResponse();

            int statusCode = resp.getStatusLine().getStatusCode();
            
            if (statusCode != HttpStatus.SC_OK) {
                throw new AbortException("[CodeSonar] failed to sign out.");
            }
        } catch (IOException ex) {
            throw new AbortException(String.format("[CodeSonar] Failed to sign out.%n[CodeSonar] Message is: %s", ex.getMessage()));
        }
    }
}
