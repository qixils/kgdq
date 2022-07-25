package dev.qixils.horaro.models

/**
 * An object that can be identifying by an id, name, slug, and link.
 *
 * @property id   the id of the object
 * @property name the name of the object
 * @property slug the slug of the object
 * @property link the public-facing link to the object's page
 */
sealed interface Identifiable {

    /**
     * The id of the object.
     */
    val id: String

    /**
     * The name of the object.
     */
    val name: String

    /**
     * The slug (short name) of the object.
     */
    val slug: String

    /**
     * The link to the object's page in the user-facing UI.
     */
    val link: String
}