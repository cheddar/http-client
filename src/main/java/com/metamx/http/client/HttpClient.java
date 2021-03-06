/*
 * Copyright 2015 Yahoo!
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.http.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.metamx.http.client.response.HttpResponseHandler;

/**
 * Interface for Async HTTP client libraries.
 */
public interface HttpClient {
  /**
   * Submit a request and process the response with the given response handler.
   *
   * Note that the Request object passed in to the HttpClient *may* be mutated by the actual client.  This is
   * largely done by composed clients, but the contract is that mutation is possible.  It is the caller's
   * responsibility to pass in a copy of the Request object if they want to have an object that is not mutated.
   *
   * @param request Request to process, this *may* be mutated by the client
   * @param handler An asynchronous response handler that will be used to process results from the http call
   * @param <Intermediate> The type of the intermediate results from the handler
   * @param <Final> The type of the final results that the returned ListenableFuture will contain
   * @return A listenable future that will eventually provide an object of type Final
   */
  public <Intermediate, Final> ListenableFuture<Final> go(
      Request request,
      HttpResponseHandler<Intermediate, Final> handler
  );
}
