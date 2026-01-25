/**
 * Scent Trigger System for Aroma Affect.
 * 
 * <p>This package provides a centralized system for triggering scents to OVR hardware
 * based on in-game events such as item use, biome changes, block interactions, etc.</p>
 * 
 * <h2>Architecture Overview</h2>
 * <pre>
 * scent_triggers.json → ScentTriggerConfigLoader → Validation
 *                              ↓
 * Game Events → ScentTriggerHandler → ScentTriggerManager → OvrWebSocketClient
 * </pre>
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.trigger.ScentTriggerSource} - Enum defining trigger sources</li>
 *   <li>{@link com.ovrtechnology.trigger.ScentPriority} - Enum for scent priority levels</li>
 *   <li>{@link com.ovrtechnology.trigger.ScentTrigger} - Record representing an active trigger</li>
 *   <li>{@link com.ovrtechnology.trigger.ScentTriggerManager} - Core singleton managing active scents</li>
 *   <li>{@link com.ovrtechnology.trigger.ScentTriggerHandler} - Event listeners for game events</li>
 * </ul>
 * 
 * <h2>Configuration</h2>
 * <p>Triggers are configured via {@code data/aromaaffect/scents/scent_triggers.json}.
 * The config subpackage contains POJOs for loading this configuration.</p>
 * 
 * <h2>WebSocket Protocol</h2>
 * <p>Messages sent to OVR hardware follow this format:</p>
 * <ul>
 *   <li>Play scent: {@code {"reset":false,"scent":"ScentName"}}</li>
 *   <li>Stop scent: {@code {"reset":true,"scent":"ScentName"}}</li>
 * </ul>
 * 
 * @see com.ovrtechnology.websocket.OvrWebSocketClient
 */
package com.ovrtechnology.trigger;
