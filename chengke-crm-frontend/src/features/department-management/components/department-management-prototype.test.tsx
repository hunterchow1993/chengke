import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { DepartmentManagementPrototype } from './department-management-prototype';

describe('DepartmentManagementPrototype', () => {
  it('shows the default department context and its organization impact', () => {
    render(<DepartmentManagementPrototype />);

    expect(screen.getByRole('heading', { name: '华东事业部' })).toBeInTheDocument();
    expect(screen.getByText('east_division')).toBeInTheDocument();
    expect(screen.getByText('澄客集团 / 华东事业部')).toBeInTheDocument();
    expect(screen.queryByText('澄客集团 / 澄客集团 / 华东事业部')).not.toBeInTheDocument();
    expect(screen.getByText('当前节点及下级数据范围')).toBeInTheDocument();
    expect(screen.getByRole('tree', { name: '部门组织树' })).toBeInTheDocument();
  });

  it('changes the detail panel when an organization node is selected', async () => {
    const user = userEvent.setup();
    render(<DepartmentManagementPrototype />);

    await user.click(screen.getByRole('treeitem', { name: /集团财务部/ }));

    expect(screen.getByRole('heading', { name: '集团财务部' })).toBeInTheDocument();
    expect(screen.getByText('group_finance')).toBeInTheDocument();
  });

  it('searches departments while preserving their ancestor context', async () => {
    const user = userEvent.setup();
    render(<DepartmentManagementPrototype />);

    await user.type(screen.getByRole('searchbox', { name: '搜索部门' }), '杭州');

    const results = screen.getByRole('region', { name: '部门搜索结果' });
    expect(within(results).getByText('杭州销售部')).toBeInTheDocument();
    expect(within(results).getByText('澄客集团 / 华东事业部')).toBeInTheDocument();
  });

  it('validates the department name in the create drawer', async () => {
    const user = userEvent.setup();
    render(<DepartmentManagementPrototype />);

    await user.click(screen.getByRole('button', { name: '新增下级部门' }));
    expect(screen.getByRole('dialog', { name: '新增部门' })).toBeInTheDocument();

    await user.clear(screen.getByLabelText('部门名称'));
    await user.click(screen.getByRole('button', { name: '保存部门' }));

    expect(screen.getByText('请输入 2-50 个字符的部门名称')).toBeInTheDocument();
  });

  it('adds a prototype child department and confirms the local-only change', async () => {
    const user = userEvent.setup();
    render(<DepartmentManagementPrototype />);

    await user.click(screen.getByRole('button', { name: '新增下级部门' }));
    await user.clear(screen.getByLabelText('部门名称'));
    await user.type(screen.getByLabelText('部门名称'), '宁波销售部');
    await user.clear(screen.getByLabelText('部门编码'));
    await user.type(screen.getByLabelText('部门编码'), 'ningbo_sales');
    await user.click(screen.getByRole('button', { name: '保存部门' }));

    expect(screen.getByText('宁波销售部已加入当前原型')).toBeInTheDocument();
    expect(screen.getByRole('treeitem', { name: /宁波销售部/ })).toBeInTheDocument();
    expect(screen.getByText('当前可见 10 个节点')).toBeInTheDocument();
  });

  it('shows a server-style impact summary before moving a department', async () => {
    const user = userEvent.setup();
    render(<DepartmentManagementPrototype />);

    await user.click(screen.getByRole('button', { name: '更多部门操作' }));
    await user.click(screen.getByRole('menuitem', { name: '移动部门' }));

    expect(screen.getByRole('dialog', { name: '移动部门' })).toBeInTheDocument();
    expect(screen.getByText('2 个角色的数据范围将重新计算')).toBeInTheDocument();
    expect(screen.getByText('共影响 3 个组织节点')).toBeInTheDocument();
  });
});
