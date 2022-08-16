CREATE OR ALTER PROCEDURE change_primary_key @TableName NVARCHAR(30)
AS
    begin
        DECLARE @PkConstraintName NVARCHAR(255);
        SELECT @PkConstraintName = NAME FROM SYSOBJECTS WHERE XTYPE = 'PK' AND PARENT_OBJ = OBJECT_ID(@TableName);
        IF @PkConstraintName IS NOT NULL
        begin
            EXEC('ALTER TABLE ' + @TableName + ' DROP CONSTRAINT ' + @PkConstraintName);
        end
        EXEC('ALTER TABLE ' + @TableName +  ' ADD ID INTEGER NOT NULL IDENTITY PRIMARY KEY');
    end

EXEC change_primary_key @TableName = 'REG_RESOURCE_COMMENT';

EXEC change_primary_key @TableName = 'REG_RESOURCE_RATING';

EXEC change_primary_key @TableName = 'REG_RESOURCE_TAG';

EXEC change_primary_key @TableName = 'REG_RESOURCE_PROPERTY';

EXEC change_primary_key @TableName = 'UM_SHARED_USER_ROLE';
