package de.flashheart.rlgrc.networking;

/**
 * Event handler for SSE events.
 *
 * Copyright 2021 Cisco Systems
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </pre>
 *
 *
 * https://github.com/CiscoSE/commons-networking/blob/main/commons-networking/src/main/java/com/cisco/commons/networking/EventHandler.java
 *
 * @author Liran Mendelovich
 */
public interface EventHandler {

	/**
	 * Handle the event text.
	 * @param eventText - trimmed event text
	 */
	void handle(String eventText);
}