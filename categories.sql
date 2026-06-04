USE MarketDayDB;
GO

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'餐飲美食', N'food', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'food'
);

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'文創手作', N'handmade', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'handmade'
);

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'親子家庭', N'family', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'family'
);

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'寵物生活', N'pet-life', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'pet-life'
);

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'植物選物', N'plants', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'plants'
);

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'服飾配件', N'fashion-accessories', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'fashion-accessories'
);

INSERT INTO dbo.categories (name, slug, is_active)
SELECT N'玩具選物', N'toys', 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'toys'
);
GO
