/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Queries a shape index for effective traits bound to shapes and members.
 */
public final class EffectiveTraitQuery implements ToSmithyBuilder<EffectiveTraitQuery> {

    private final ShapeIndex shapeIndex;
    private final Class<? extends Trait> traitClass;
    private final boolean inheritFromContainer;

    private EffectiveTraitQuery(Builder builder) {
        this.shapeIndex = SmithyBuilder.requiredState("shapeIndex", builder.shapeIndex);
        this.traitClass = SmithyBuilder.requiredState("traitClass", builder.traitClass);
        this.inheritFromContainer = builder.inheritFromContainer;
    }

    /**
     * Checks if the trait is effectively applied to a shape.
     *
     * @param shapeId Shape to test.
     * @return Returns true if the trait is effectively applied to the shape.
     */
    public boolean isTraitApplied(ToShapeId shapeId) {
        Shape shape = shapeIndex.getShape(shapeId.toShapeId()).orElse(null);

        if (shape == null) {
            return false;
        }

        if (shape.getMemberTrait(shapeIndex, traitClass).isPresent()) {
            return true;
        }

        if (!inheritFromContainer || !shape.asMemberShape().isPresent()) {
            return false;
        }

        // Check if the parent of the member is marked with the trait.
        MemberShape memberShape = shape.asMemberShape().get();
        Shape parent = shapeIndex.getShape(memberShape.getContainer()).orElse(null);
        return parent != null && parent.hasTrait(traitClass);
    }

    /**
     * Creates a new query builder.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .shapeIndex(shapeIndex)
                .traitClass(traitClass)
                .inheritFromContainer(inheritFromContainer);
    }

    /**
     * Builds a reusable EffectiveTraitQuery.
     */
    public static final class Builder implements SmithyBuilder<EffectiveTraitQuery> {

        private ShapeIndex shapeIndex;
        private Class<? extends Trait> traitClass;
        private boolean inheritFromContainer;

        @Override
        public EffectiveTraitQuery build() {
            return new EffectiveTraitQuery(this);
        }

        @Deprecated
        public Builder shapeIndex(ShapeIndex shapeIndex) {
            this.shapeIndex = shapeIndex;
            return this;
        }

        /**
         * Sets the required model to query.
         *
         * @param model Model to query.
         * @return Returns the query object builder.
         */
        public Builder model(Model model) {
            return shapeIndex(model.getShapeIndex());
        }

        /**
         * Sets the required trait being queried.
         *
         * @param traitClass Trait to detect on shapes.
         * @return Returns the query object builder.
         */
        public Builder traitClass(Class<? extends Trait> traitClass) {
            this.traitClass = traitClass;
            return this;
        }

        /**
         * When testing member shapes, also checks the container of the member for
         * the presence of a trait.
         *
         * <p>By default, traits are not inherited from a member's parent container.
         *
         * @param inheritFromContainer Set to true to inherit traits from member containers.
         * @return Returns the query object builder.
         */
        public Builder inheritFromContainer(boolean inheritFromContainer) {
            this.inheritFromContainer = inheritFromContainer;
            return this;
        }
    }
}
