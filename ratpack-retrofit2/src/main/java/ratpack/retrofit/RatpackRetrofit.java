/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.retrofit;

import com.google.common.base.Preconditions;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.retrofit.internal.RatpackCallAdapterFactory;
import ratpack.retrofit.internal.RatpackCallFactory;
import ratpack.util.Exceptions;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.net.URI;

/**
 * Builder for providing integration of Retrofit2 with Ratpack's {@link HttpClient}.
 * <p>
 * This class allows for creating declarative type-safe interfaces that represent remote HTTP APIs.
 * Using this adapter allows for defining the interfaces to return {@link ratpack.exec.Promise} types which will be fulfilled by Ratpack's http client.
 *
 * <pre class="java">{@code
 * import retrofit2.http.BODY;
 * import retrofit2.http.GET;
 * import retrofit2.http.POST;
 *
 * public class ExampleRetrofitClient {
 *
 *   public interface HelloService {
 *
 *     {@literal @}GET("/hello") Promise<String> hello();
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *
 *     EmbeddedApp.fromHandlers(chain -> {
 *       chain
 *         .get(ctx -> {
 *           PublicAddress address = ctx.get(PublicAddress.class);
 *           HttpClient httpClient = ctx.get(HttpClient.class);
 *           HelloService service = RatpackRetrofit.builder(httpClient)
 *             .uri(address.get())
 *             .build(HelloService.class);
 *
 *           ctx.render service.hello();
 *         })
 *         .get("hello", ctx -> {
 *           ctx.render("hello")
 *         })
 *       }
 *     ).test(testHttpClient -> {
 *       assertEquals("hello", testHttpClient.getText());
 *     }
 *   }
 * }
 * }</pre>
 *
 */
public class RatpackRetrofit {

  /**
   * Create a new builder for creating Retrofit implementations.
   *
   * @param httpClient the http client for Retrofit to use to make requests.
   *
   * @return a builder
   */
  public static Builder builder(HttpClient httpClient) {
    return new Builder(httpClient);
  }

  public static class Builder {

    private HttpClient httpClient;
    private Action<? super Retrofit.Builder> builderAction = Action.noop();
    private URI uri;

    public Builder(HttpClient httpClient) {
      this.httpClient = httpClient;
    }

    /**
     * Configure the underlying {@link retrofit2.Retrofit.Builder} instance.
     * <p>
     * This is used to customize the behavior of Retrofit.
     *
     * @param builderAction the actions to apply to the Retrofit builder
     * @return this
     * @see Converter.Factory
     * @see retrofit2.CallAdapter.Factory
     */
    public Builder configure(Action<? super Retrofit.Builder> builderAction) {
      this.builderAction = builderAction;
      return this;
    }

    /**
     * Sets the base URI for all requests issued by Retrofit clients created by this builder.
     * <p>
     * This must be specified.
     * @param uri The base URI for http requests
     * @return this
     */
    public Builder uri(URI uri) {
      this.uri = uri;
      return this;
    }

    /**
     * Sets the base URI for all requests issued by Retrofit clients created by this builder.
     * <p>
     * This must be specified.
     * @param uri The base URI for http requests
     * @return this
     */
    public Builder uri(String uri) {
      return Exceptions.uncheck(() -> uri(new URI(uri)));
    }

    /**
     * Creates the underlying {@link Retrofit} instance and configures it to interface with {@link HttpClient} and {@link ratpack.exec.Promise}.
     * <p>
     * The resulting Retrofit instance can be re-used to generate multiple client interfaces which share the same base URI.
     * @return the Retrofit instance to create client interfaces
     */
    public Retrofit retrofit() {
      Preconditions.checkNotNull(httpClient, "Must provide a HttpClient instance.");
      Preconditions.checkNotNull(uri, "Must provide the base uri.");
      Retrofit.Builder builder = new Retrofit.Builder()
        .callFactory(new RatpackCallFactory(httpClient))
        .addCallAdapterFactory(RatpackCallAdapterFactory.INSTANCE)
        .addConverterFactory(ScalarsConverterFactory.create());
      builder.baseUrl(uri.toString());
      Exceptions.uncheck(() -> builderAction.execute(builder));
      return builder.build();
    }

    /**
     * Uses this builder to create a Retrofit client implemention.
     * <p>
     * This is the short form of calling {@code builder.retrofit().build(service)}.
     *
     * @param service the client interface to generate.
     * @param <T> the type of the client interface.
     * @return a generated instance of the client interface.
     */
    public <T> T build(Class<T> service) {
      return retrofit().create(service);
    }
  }

}