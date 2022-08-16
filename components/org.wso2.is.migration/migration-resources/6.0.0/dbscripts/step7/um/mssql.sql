CREATE OR ALTER PROCEDURE ChangePrimaryKey @TableName NVARCHAR(255)
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

GO

EXEC ChangePrimaryKey @TableName = 'REG_RESOURCE_COMMENT';

EXEC ChangePrimaryKey @TableName = 'REG_RESOURCE_RATING';

EXEC ChangePrimaryKey @TableName = 'REG_RESOURCE_TAG';

EXEC ChangePrimaryKey @TableName = 'REG_RESOURCE_PROPERTY';

EXEC ChangePrimaryKey @TableName = 'UM_SHARED_USER_ROLE';
