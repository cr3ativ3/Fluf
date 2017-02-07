package fluf;

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binding provider annotation. Its value can be used as a provider name qualifier
 * and <code>cache</code> attribute if set to true (false by default) tells if this provider should
 * cache its result.
 *
 * <p>Example usage:
 *
 * <pre>
 *   public class VehicleModule extends Module {
 *   
 *     <b>@Bind("driver")</b>
 *     Seat driverSeat(){
 *     	...
 *     };
 *     
 *     <b>@Bind</b>
 *     Seat passengerSeat(){
 *     	...
 *     };
 *     
 *     <b>@Bind</b>
 *     Engine provideEngine(){
 *     	...
 *     };
 *     
 *     <b>@Bind(cache = true)</b>
 *     Car transporter(<b>@Bind</b> Engine engine, <b>@Bind("driver")</b> Seat seat1, <b>@Bind("passengerSeat")</b> Seat seat2){
 *     	...
 *     };
 *   }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Bind {
	String value() default "";
	boolean cache() default false;
}