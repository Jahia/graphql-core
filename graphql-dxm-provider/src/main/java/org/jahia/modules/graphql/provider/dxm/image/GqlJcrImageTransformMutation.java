package org.jahia.modules.graphql.provider.dxm.image;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.image.Image;
import org.jahia.services.image.JahiaImageService;
import pl.touk.throwing.ThrowingBiConsumer;
import pl.touk.throwing.exception.WrappedException;

import javax.jcr.RepositoryException;
import java.io.*;
import java.util.function.BiConsumer;

public class GqlJcrImageTransformMutation {

    private JCRNodeWrapper node;
    private String name;
    private String targetPath;
    private JahiaImageService imageService;

    public GqlJcrImageTransformMutation(JCRNodeWrapper node, String name, String targetPath) throws RepositoryException {
        this.node = node;
        this.name = name != null ? name : node.getName();
        this.targetPath = targetPath != null ? targetPath : node.getParent().getPath();
        this.imageService = BundleUtils.getOsgiService(JahiaImageService.class, null);
    }

    /**
     * @return The transformed node
     */
    @GraphQLField
    @GraphQLDescription("The transformed node")
    public GqlJcrNode getNode() {
        try {
            return SpecializedTypesHandler.getNode(node);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @param angle angle
     * @return always true
     */
    @GraphQLField
    @GraphQLDescription("Rotate an image under the current node")
    public boolean rotateImage(
            @GraphQLName("angle") @GraphQLNonNull @GraphQLDescription("angle in degrees") double angle) {
        JCRNodeWrapper result;
        try {
            this.node = doOperation(node, ThrowingBiConsumer.unchecked((image,f) -> imageService.rotateImage(image, f, angle)));
        } catch (WrappedException e) {
            throw new DataFetchingException(e.getCause());
        }
        return true;
    }

    /**
     * @param height height of the new image
     * @param width  width of the new image
     * @return always true
     */
    @GraphQLField
    @GraphQLDescription("Resize an image under the current node")
    public boolean resizeImage(
            @GraphQLName("height") @GraphQLNonNull @GraphQLDescription("new height") int height,
            @GraphQLName("width") @GraphQLNonNull @GraphQLDescription("new width") int width) {
        JCRNodeWrapper result;
        try {
            this.node = doOperation(node, ThrowingBiConsumer.unchecked((image,f) -> imageService.resizeImage(image, f, width, height, JahiaImageService.ResizeType.SCALE_TO_FILL)));
        } catch (WrappedException e) {
            throw new DataFetchingException(e.getCause());
        }
        return true;
    }

    /**
     * @param height height of the new image
     * @param width  width of the new image
     * @param top    top of the new image
     * @param left   left of the new image
     * @return always true
     */
    @GraphQLField
    @GraphQLDescription("Crop an image under the current node")
    public boolean cropImage(
            @GraphQLName("height") @GraphQLNonNull @GraphQLDescription("new height") int height,
            @GraphQLName("width") @GraphQLNonNull @GraphQLDescription("new width") int width,
            @GraphQLName("top") @GraphQLNonNull @GraphQLDescription("top") int top,
            @GraphQLName("left") @GraphQLNonNull @GraphQLDescription("left") int left) {
        JCRNodeWrapper result;
        try {
            this.node = doOperation(node, ThrowingBiConsumer.unchecked((image,f) -> imageService.cropImage(image, f, top, left, width, height)));
        } catch (WrappedException e) {
            throw new DataFetchingException(e.getCause());
        }
        return true;
    }


    /**
     * @param operation operation to execute
     */
    private JCRNodeWrapper doOperation(JCRNodeWrapper jcrNode, BiConsumer<Image, File> operation) {
        InputStream fis = null;
        File f = null;
        try {
            Image image = imageService.getImage(jcrNode);

            String fileExtension = FilenameUtils.getExtension(name);
            if ((fileExtension != null) && (!fileExtension.equals(""))) {
                fileExtension = "." + fileExtension;
            } else {
                fileExtension = null;
            }
            f = File.createTempFile("image", fileExtension);
            operation.accept(image, f);

            fis = new BufferedInputStream(new FileInputStream(f));

            return jcrNode.getSession().getNode(targetPath).uploadFile(name, fis, jcrNode.getFileContent().getContentType());
        } catch (IOException | RepositoryException e) {
            throw new DataFetchingException(e);
        } finally {
            IOUtils.closeQuietly(fis);
            if (f != null) {
                f.delete();
            }
        }
    }

}
