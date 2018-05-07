package org.mycore.services.kv;

public enum MCRKVStrategy {
    /**
     * If you access the the key it will be stored if not exist.
     */
    DEMAND,
    /**
     * If you update, create, delete a object the key will be stored.
     */
    UPDATE
}
