import { Table, Pagination } from 'antd';

/**
 * 分页表格（后端分页：{list, total, page, size}）。
 * 受控分页：change 后由父组件重新拉取数据。
 */
export default function PaginatedTable({
  rowKey = 'id',
  columns,
  dataSource = [],
  total = 0,
  page = 1,
  size = 10,
  onPageChange,
  loading = false,
  sizeOptions = [10, 20, 50],
  expandable,
}) {
  return (
    <>
      <Table
        rowKey={rowKey}
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        expandable={expandable}
        scroll={{ x: 'max-content' }}
        size="middle"
      />
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
        <Pagination
          current={page}
          pageSize={size}
          total={total}
          showSizeChanger
          showTotal={(t) => `共 ${t} 条`}
          pageSizeOptions={sizeOptions}
          onChange={(p, s) => onPageChange(p, s)}
        />
      </div>
    </>
  );
}
